resource "aws_apigatewayv2_api" "main" {
  name          = "${var.name}-http-api"
  protocol_type = "HTTP"

  tags = var.tags
}

resource "aws_apigatewayv2_vpc_link" "alb" {
  name               = "${var.name}-alb-vpc-link"
  subnet_ids         = var.vpc_link_subnet_ids
  security_group_ids = var.vpc_link_security_group_ids

  tags = var.tags
}

resource "aws_apigatewayv2_integration" "alb" {
  api_id             = aws_apigatewayv2_api.main.id
  integration_type   = "HTTP_PROXY"
  integration_method = "ANY"
  integration_uri    = var.alb_listener_arn
  connection_type    = "VPC_LINK"
  connection_id      = aws_apigatewayv2_vpc_link.alb.id
}

resource "aws_apigatewayv2_route" "api_proxy" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "ANY /api/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.alb.id}"
}

# HTTP API(v2) does not support S3 GetObject as a first-class AWS service
# integration. This REST API(v1) is a narrow S3 reader; the public entrypoint
# remains the HTTP API default execute-api endpoint.
resource "aws_api_gateway_rest_api" "frontend" {
  name               = "${var.name}-frontend-s3-proxy"
  binary_media_types = ["*/*"]

  endpoint_configuration {
    types = ["REGIONAL"]
  }

  tags = var.tags
}

resource "aws_api_gateway_resource" "frontend_proxy" {
  rest_api_id = aws_api_gateway_rest_api.frontend.id
  parent_id   = aws_api_gateway_rest_api.frontend.root_resource_id
  path_part   = "{proxy+}"
}

resource "aws_api_gateway_method" "frontend_root_get" {
  rest_api_id   = aws_api_gateway_rest_api.frontend.id
  resource_id   = aws_api_gateway_rest_api.frontend.root_resource_id
  http_method   = "GET"
  authorization = "NONE"
}

resource "aws_api_gateway_method" "frontend_proxy_get" {
  rest_api_id   = aws_api_gateway_rest_api.frontend.id
  resource_id   = aws_api_gateway_resource.frontend_proxy.id
  http_method   = "GET"
  authorization = "NONE"

  request_parameters = {
    "method.request.path.proxy" = true
  }
}

resource "aws_api_gateway_integration" "frontend_root" {
  rest_api_id             = aws_api_gateway_rest_api.frontend.id
  resource_id             = aws_api_gateway_rest_api.frontend.root_resource_id
  http_method             = aws_api_gateway_method.frontend_root_get.http_method
  integration_http_method = "GET"
  type                    = "AWS"
  uri                     = "arn:aws:apigateway:${var.region}:s3:path/${var.frontend_bucket_name}/index.html"
  credentials             = var.integration_role_arn
  passthrough_behavior    = "WHEN_NO_MATCH"
}

resource "aws_api_gateway_integration" "frontend_proxy" {
  rest_api_id             = aws_api_gateway_rest_api.frontend.id
  resource_id             = aws_api_gateway_resource.frontend_proxy.id
  http_method             = aws_api_gateway_method.frontend_proxy_get.http_method
  integration_http_method = "GET"
  type                    = "AWS"
  uri                     = "arn:aws:apigateway:${var.region}:s3:path/${var.frontend_bucket_name}/{object}"
  credentials             = var.integration_role_arn
  passthrough_behavior    = "WHEN_NO_MATCH"

  request_parameters = {
    "integration.request.path.object" = "method.request.path.proxy"
  }
}

resource "aws_api_gateway_method_response" "frontend_root_200" {
  rest_api_id = aws_api_gateway_rest_api.frontend.id
  resource_id = aws_api_gateway_rest_api.frontend.root_resource_id
  http_method = aws_api_gateway_method.frontend_root_get.http_method
  status_code = "200"

  response_parameters = {
    "method.response.header.Content-Type" = true
  }
}

resource "aws_api_gateway_method_response" "frontend_proxy_200" {
  rest_api_id = aws_api_gateway_rest_api.frontend.id
  resource_id = aws_api_gateway_resource.frontend_proxy.id
  http_method = aws_api_gateway_method.frontend_proxy_get.http_method
  status_code = "200"

  response_parameters = {
    "method.response.header.Content-Type" = true
  }
}

resource "aws_api_gateway_integration_response" "frontend_root_200" {
  rest_api_id = aws_api_gateway_rest_api.frontend.id
  resource_id = aws_api_gateway_rest_api.frontend.root_resource_id
  http_method = aws_api_gateway_method.frontend_root_get.http_method
  status_code = aws_api_gateway_method_response.frontend_root_200.status_code

  response_parameters = {
    "method.response.header.Content-Type" = "integration.response.header.Content-Type"
  }

  depends_on = [aws_api_gateway_integration.frontend_root]
}

resource "aws_api_gateway_integration_response" "frontend_proxy_200" {
  rest_api_id = aws_api_gateway_rest_api.frontend.id
  resource_id = aws_api_gateway_resource.frontend_proxy.id
  http_method = aws_api_gateway_method.frontend_proxy_get.http_method
  status_code = aws_api_gateway_method_response.frontend_proxy_200.status_code

  response_parameters = {
    "method.response.header.Content-Type" = "integration.response.header.Content-Type"
  }

  depends_on = [aws_api_gateway_integration.frontend_proxy]
}

resource "aws_api_gateway_deployment" "frontend" {
  rest_api_id = aws_api_gateway_rest_api.frontend.id

  triggers = {
    redeployment = sha1(jsonencode([
      aws_api_gateway_resource.frontend_proxy.id,
      aws_api_gateway_method.frontend_root_get.id,
      aws_api_gateway_method.frontend_proxy_get.id,
      aws_api_gateway_integration.frontend_root.id,
      aws_api_gateway_integration.frontend_proxy.id,
      aws_api_gateway_integration_response.frontend_root_200.id,
      aws_api_gateway_integration_response.frontend_proxy_200.id,
    ]))
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_api_gateway_stage" "frontend" {
  rest_api_id   = aws_api_gateway_rest_api.frontend.id
  deployment_id = aws_api_gateway_deployment.frontend.id
  stage_name    = "s3"
}

resource "aws_s3_bucket_policy" "frontend_read" {
  bucket = var.frontend_bucket_name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "AllowLabRoleReadViaApiGatewayIntegration"
        Effect    = "Allow"
        Principal = { AWS = var.integration_role_arn }
        Action    = "s3:GetObject"
        Resource  = "${var.frontend_bucket_arn}/*"
      }
    ]
  })
}

resource "aws_apigatewayv2_integration" "frontend_root" {
  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "${aws_api_gateway_stage.frontend.invoke_url}/"
  payload_format_version = "1.0"

  request_parameters = {
    "overwrite:path" = "/s3/"
  }
}

resource "aws_apigatewayv2_integration" "frontend_assets" {
  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "${aws_api_gateway_stage.frontend.invoke_url}/"
  payload_format_version = "1.0"

  request_parameters = {
    "overwrite:path" = "/s3/assets/$request.path.proxy"
  }
}

resource "aws_apigatewayv2_route" "frontend_assets" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /assets/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.frontend_assets.id}"
}

resource "aws_apigatewayv2_route" "frontend_default" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "$default"
  target    = "integrations/${aws_apigatewayv2_integration.frontend_root.id}"
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.main.id
  name        = "$default"
  auto_deploy = true

  tags = var.tags
}

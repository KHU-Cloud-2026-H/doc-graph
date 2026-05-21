variable "private_subnet_ids" {
  type = list(string)
}

variable "sg_id" {
  type = string
}

variable "db_password" {
  type      = string
  sensitive = true
}

variable "db_username" {
  type    = string
  default = "docgraph"
}

variable "db_name" {
  type    = string
  default = "docgraph"
}

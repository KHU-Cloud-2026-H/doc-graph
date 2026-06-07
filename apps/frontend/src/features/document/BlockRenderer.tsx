import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { FileText, Database, Info, Square } from "lucide-react";
import type { components } from "@docgraph/api-types";
import { blocksToTree, type BlockTreeNode } from "../../lib/blocksToTree";

type Block = components["schemas"]["Block"];

interface BlockRendererProps {
  blocks: Block[];
  notionPageIdToDocId?: Map<string, number>;
  docBasePath?: string;
}

export function BlockRenderer({ blocks, notionPageIdToDocId, docBasePath }: BlockRendererProps) {
  const tree = blocksToTree(blocks);
  return (
    <div className="pb-32 [&>*:first-child]:mt-0">
      {renderSiblings(tree, notionPageIdToDocId, docBasePath)}
    </div>
  );
}

function renderSiblings(
  nodes: BlockTreeNode[],
  notionPageIdToDocId?: Map<string, number>,
  docBasePath?: string,
): ReactNode {
  const output: ReactNode[] = [];
  let i = 0;

  while (i < nodes.length) {
    const node = nodes[i];

    if (node.type === "bulleted_list_item") {
      const group: BlockTreeNode[] = [];
      while (i < nodes.length && nodes[i].type === "bulleted_list_item") {
        group.push(nodes[i]);
        i++;
      }
      output.push(
        <ul key={`ul-${group[0].blockId}`} className="my-3 list-disc pl-6 space-y-1">
          {group.map((n) => (
            <li key={n.blockId}>
              {n.text}
              {n.children.length > 0 && renderSiblings(n.children, notionPageIdToDocId, docBasePath)}
            </li>
          ))}
        </ul>
      );
      continue;
    }

    if (node.type === "numbered_list_item") {
      const group: BlockTreeNode[] = [];
      while (i < nodes.length && nodes[i].type === "numbered_list_item") {
        group.push(nodes[i]);
        i++;
      }
      output.push(
        <ol key={`ol-${group[0].blockId}`} className="my-3 list-decimal pl-6 space-y-1">
          {group.map((n) => (
            <li key={n.blockId}>
              {n.text}
              {n.children.length > 0 && renderSiblings(n.children, notionPageIdToDocId, docBasePath)}
            </li>
          ))}
        </ol>
      );
      continue;
    }

    if (node.type === "child_page" || node.type === "child_database") {
      const group: BlockTreeNode[] = [];
      while (
        i < nodes.length &&
        (nodes[i].type === "child_page" || nodes[i].type === "child_database")
      ) {
        group.push(nodes[i]);
        i++;
      }
      output.push(
        <div key={`child-group-${group[0].blockId}`} className="my-3 space-y-0.5">
          {group.map((n) => {
            // blockId == Notion page ID → 내부 문서 ID 매핑
            const normalizedId = n.blockId.replace(/-/g, '');
            const internalId = notionPageIdToDocId?.get(n.blockId)
              ?? notionPageIdToDocId?.get(normalizedId);
            const Icon = n.type === "child_page" ? FileText : Database;

            if (internalId !== undefined && docBasePath) {
              return (
                <Link
                  key={n.blockId}
                  to={`${docBasePath}/${internalId}`}
                  className="flex items-center gap-1.5 px-1.5 py-1 rounded hover:bg-slate-100 text-[15px] text-blue-600 hover:text-blue-700 transition-colors"
                >
                  <Icon className="w-4 h-4 shrink-0" />
                  {n.text}
                </Link>
              );
            }
            return (
              <div
                key={n.blockId}
                className="flex items-center gap-1.5 px-1.5 py-1 rounded hover:bg-slate-100 text-[15px] cursor-default text-slate-700"
              >
                <Icon className="w-4 h-4 shrink-0" />
                {n.text}
              </div>
            );
          })}
        </div>
      );
      continue;
    }

    if (node.type === "toggle") {
      output.push(
        <details key={node.blockId} className="my-2">
          <summary className="cursor-pointer font-medium">{node.text}</summary>
          <div className="pl-4 mt-1.5 space-y-1.5 [&>*]:!my-0">
            {renderSiblings(node.children)}
          </div>
        </details>
      );
      i++;
      continue;
    }

    output.push(
      <div key={node.blockId} className={blockMarginClass(node.type)}>
        {renderBlock(node)}
        {node.children.length > 0 && (
          <div className="pl-4">{renderSiblings(node.children)}</div>
        )}
      </div>
    );
    i++;
  }

  return output;
}

function blockMarginClass(type: string): string {
  switch (type) {
    case "paragraph":  return "mb-3";
    case "heading_1":  return "mt-8 mb-3";
    case "heading_2":  return "mt-8 mb-2";
    case "heading_3":  return "mt-5 mb-1";
    case "heading_4":  return "mt-4 mb-1";
    case "quote":      return "my-3";
    case "divider":    return "my-6";
    case "code":       return "my-3";
    case "callout":    return "my-3";
    case "to_do":      return "my-0.5";
    default:           return "my-2";
  }
}

function renderBlock(node: BlockTreeNode): ReactNode {
  const { type, text } = node;

  switch (type) {
    case "paragraph":
      return <p>{text}</p>;
    case "heading_1":
      return <h1 className="text-3xl font-bold">{text}</h1>;
    case "heading_2":
      return <h2 className="text-2xl font-bold">{text}</h2>;
    case "heading_3":
      return <h3 className="text-xl font-semibold">{text}</h3>;
    case "heading_4":
      return <h4 className="text-lg font-semibold">{text}</h4>;
    case "quote":
      return (
        <blockquote className="border-l-4 border-slate-300 pl-4 text-slate-600 italic">
          {text}
        </blockquote>
      );
    case "divider":
      return <hr className="border-slate-200" />;
    case "code":
      return (
        <pre className="bg-slate-100 rounded-md p-3 overflow-x-auto text-sm font-mono whitespace-pre">
          <code>{text}</code>
        </pre>
      );
    case "callout":
      return (
        <div className="flex items-start gap-2 bg-slate-50 border border-slate-200 rounded-md p-3">
          <span className="flex items-center h-6 shrink-0">
            <Info className="w-4 h-4 text-slate-500" />
          </span>
          <div>{text}</div>
        </div>
      );
    case "to_do":
      return (
        <div className="flex items-start gap-2">
          <span className="flex items-center h-6 shrink-0">
            <Square className="w-4 h-4 text-slate-400" />
          </span>
          <span>{text}</span>
        </div>
      );
    default:
      if (text) return <p>{text}</p>;
      return (
        <div className="text-sm text-slate-400 border border-dashed border-slate-200 rounded px-2 py-1">
          [{type} 블록 — 지원 예정]
        </div>
      );
  }
}

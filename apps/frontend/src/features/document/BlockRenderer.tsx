import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { FileText, Database, Info, Square, AlertTriangle } from "lucide-react";
import type { components } from "@docgraph/api-types";
import { blocksToTree, type BlockTreeNode } from "../../lib/blocksToTree";

type Block = components["schemas"]["Block"];

interface BlockRendererProps {
  blocks: Block[];
  notionPageIdToDocId?: Map<string, number>;
  docBasePath?: string;
  conflictData?: Map<string, { newText?: string }>;
  onConflictBlockClick?: () => void;
  isSidebarOpen?: boolean;
}

export function BlockRenderer({ blocks, notionPageIdToDocId, docBasePath, conflictData, onConflictBlockClick, isSidebarOpen }: BlockRendererProps) {
  const tree = blocksToTree(blocks);
  return (
    <div className="pb-32 [&>*:first-child]:mt-0">
      {renderSiblings(tree, notionPageIdToDocId, docBasePath, conflictData, onConflictBlockClick, isSidebarOpen)}
    </div>
  );
}

function diffBlock(
  text: string | null,
  conflictEntry: { newText?: string },
): ReactNode {
  return (
    <div className="border border-slate-200 rounded-lg overflow-hidden my-1">
      <div className="bg-red-50 px-3 py-2 flex items-start gap-2">
        <span className="bg-red-500 text-white rounded-full w-4 h-4 flex items-center justify-center text-[11px] font-bold shrink-0 mt-0.5">-</span>
        <span className="whitespace-pre-wrap" style={{ font: 'inherit' }}>{text}</span>
      </div>
      {conflictEntry.newText && (
        <div className="bg-green-50 px-3 py-2 flex items-start gap-2 border-t border-slate-100">
          <span className="bg-green-600 text-white rounded-full w-4 h-4 flex items-center justify-center text-[11px] font-bold shrink-0 mt-0.5">+</span>
          <span className="whitespace-pre-wrap" style={{ font: 'inherit' }}>{conflictEntry.newText}</span>
        </div>
      )}
    </div>
  );
}

function renderChildPageItem(
  n: BlockTreeNode,
  notionPageIdToDocId?: Map<string, number>,
  docBasePath?: string,
): ReactNode {
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
}

function renderSiblings(
  nodes: BlockTreeNode[],
  notionPageIdToDocId?: Map<string, number>,
  docBasePath?: string,
  conflictData?: Map<string, { newText?: string }>,
  onConflictBlockClick?: () => void,
  isSidebarOpen?: boolean,
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
          {group.map((n) => {
            const conflictEntry = conflictData?.get(n.blockId);
            const isConflict = !!conflictEntry;
            if (isConflict && !isSidebarOpen) {
              return (
                <li key={n.blockId} className="list-none relative cursor-pointer" onClick={onConflictBlockClick}>
                  <AlertTriangle className="absolute -left-5 top-1 w-4 h-4 text-red-400" />
                  <span className="bg-red-50 text-red-700 rounded border-b-2 border-red-400 px-1 whitespace-pre-wrap">{n.text}</span>
                  {n.children.length > 0 && renderSiblings(n.children, notionPageIdToDocId, docBasePath, conflictData, onConflictBlockClick, isSidebarOpen)}
                </li>
              );
            }
            if (isConflict && isSidebarOpen) {
              return (
                <li key={n.blockId} className="list-none">
                  {diffBlock(n.text, conflictEntry)}
                  {n.children.length > 0 && renderSiblings(n.children, notionPageIdToDocId, docBasePath, conflictData, onConflictBlockClick, isSidebarOpen)}
                </li>
              );
            }
            return (
              <li key={n.blockId}>
                <span className="whitespace-pre-wrap">{n.text}</span>
                {n.children.length > 0 && renderSiblings(n.children, notionPageIdToDocId, docBasePath, conflictData, onConflictBlockClick, isSidebarOpen)}
              </li>
            );
          })}
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
          {group.map((n) => {
            const conflictEntry = conflictData?.get(n.blockId);
            const isConflict = !!conflictEntry;
            if (isConflict && !isSidebarOpen) {
              return (
                <li key={n.blockId} className="list-none relative cursor-pointer" onClick={onConflictBlockClick}>
                  <AlertTriangle className="absolute -left-5 top-1 w-4 h-4 text-red-400" />
                  <span className="bg-red-50 text-red-700 rounded border-b-2 border-red-400 px-1 whitespace-pre-wrap">{n.text}</span>
                  {n.children.length > 0 && renderSiblings(n.children, notionPageIdToDocId, docBasePath, conflictData, onConflictBlockClick, isSidebarOpen)}
                </li>
              );
            }
            if (isConflict && isSidebarOpen) {
              return (
                <li key={n.blockId} className="list-none">
                  {diffBlock(n.text, conflictEntry)}
                  {n.children.length > 0 && renderSiblings(n.children, notionPageIdToDocId, docBasePath, conflictData, onConflictBlockClick, isSidebarOpen)}
                </li>
              );
            }
            return (
              <li key={n.blockId}>
                <span className="whitespace-pre-wrap">{n.text}</span>
                {n.children.length > 0 && renderSiblings(n.children, notionPageIdToDocId, docBasePath, conflictData, onConflictBlockClick, isSidebarOpen)}
              </li>
            );
          })}
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
            const conflictEntry = conflictData?.get(n.blockId);
            const isConflict = !!conflictEntry;

            if (isConflict && !isSidebarOpen) {
              return (
                <div key={n.blockId} className="relative cursor-pointer" onClick={onConflictBlockClick}>
                  <AlertTriangle className="absolute -left-5 top-1 w-4 h-4 text-red-400" />
                  <div className="bg-red-50 text-red-700 rounded border-b-2 border-red-400 px-1">
                    {renderChildPageItem(n, notionPageIdToDocId, docBasePath)}
                  </div>
                </div>
              );
            }

            if (isConflict && isSidebarOpen) {
              return (
                <div key={n.blockId}>
                  {diffBlock(n.text, conflictEntry)}
                </div>
              );
            }

            return renderChildPageItem(n, notionPageIdToDocId, docBasePath);
          })}
        </div>
      );
      continue;
    }

    if (node.type === "toggle") {
      const conflictEntry = conflictData?.get(node.blockId);
      const isConflict = !!conflictEntry;
      if (isConflict && !isSidebarOpen) {
        output.push(
          <div key={node.blockId} className={`${blockMarginClass(node.type)} relative cursor-pointer`} onClick={onConflictBlockClick}>
            <AlertTriangle className="absolute -left-5 top-1 w-4 h-4 text-red-400" />
            <details>
              <summary className="cursor-pointer font-medium">
                <span className="bg-red-50 text-red-700 rounded border-b-2 border-red-400 px-1 whitespace-pre-wrap">{node.text}</span>
              </summary>
              {node.children.length > 0 && (
                <div className="pl-4 mt-1.5 space-y-1.5 [&>*]:!my-0">
                  {renderSiblings(node.children, notionPageIdToDocId, docBasePath, conflictData, onConflictBlockClick, isSidebarOpen)}
                </div>
              )}
            </details>
          </div>
        );
      } else if (isConflict && isSidebarOpen) {
        output.push(
          <div key={node.blockId} className={blockMarginClass(node.type)}>
            {diffBlock(node.text, conflictEntry)}
            {node.children.length > 0 && (
              <div className="pl-4">
                {renderSiblings(node.children, notionPageIdToDocId, docBasePath, conflictData, onConflictBlockClick, isSidebarOpen)}
              </div>
            )}
          </div>
        );
      } else {
        output.push(
          <details key={node.blockId} className="my-2">
            <summary className="cursor-pointer font-medium">{node.text}</summary>
            <div className="pl-4 mt-1.5 space-y-1.5 [&>*]:!my-0">
              {renderSiblings(node.children, notionPageIdToDocId, docBasePath, conflictData, onConflictBlockClick, isSidebarOpen)}
            </div>
          </details>
        );
      }
      i++;
      continue;
    }

    const conflictEntry = conflictData?.get(node.blockId);
    const isConflict = !!conflictEntry;

    if (isConflict && !isSidebarOpen) {
      output.push(
        <div
          key={node.blockId}
          className={`${blockMarginClass(node.type)} relative cursor-pointer`}
          onClick={onConflictBlockClick}
        >
          <AlertTriangle className="absolute -left-5 top-1 w-4 h-4 text-red-400" />
          {renderBlock(node, undefined, undefined, true)}
          {node.children.length > 0 && (
            <div className="pl-4">{renderSiblings(node.children, notionPageIdToDocId, docBasePath, conflictData, onConflictBlockClick, isSidebarOpen)}</div>
          )}
        </div>
      );
    } else {
      output.push(
        <div key={node.blockId} className={blockMarginClass(node.type)}>
          {renderBlock(node, conflictEntry, isSidebarOpen)}
          {node.children.length > 0 && (
            <div className="pl-4">{renderSiblings(node.children, notionPageIdToDocId, docBasePath, conflictData, onConflictBlockClick, isSidebarOpen)}</div>
          )}
        </div>
      );
    }
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

function renderBlock(
  node: BlockTreeNode,
  conflictEntry?: { newText?: string },
  isSidebarOpen?: boolean,
  highlightText?: boolean,
): ReactNode {
  const { type, text } = node;
  const showDiff = !!(isSidebarOpen && conflictEntry);

  const highlighted = (
    <span className="bg-red-50 text-red-700 rounded border-b-2 border-red-400 px-1 whitespace-pre-wrap">{text}</span>
  );

  switch (type) {
    case "paragraph":
      if (showDiff) return <p>{diffBlock(text, conflictEntry!)}</p>;
      if (highlightText) return <p>{highlighted}</p>;
      return <p className="whitespace-pre-wrap">{text}</p>;

    case "heading_1":
      if (showDiff) return <h1 className="text-3xl font-bold">{diffBlock(text, conflictEntry!)}</h1>;
      if (highlightText) return <h1 className="text-3xl font-bold">{highlighted}</h1>;
      return <h1 className="text-3xl font-bold whitespace-pre-wrap">{text}</h1>;

    case "heading_2":
      if (showDiff) return <h2 className="text-2xl font-bold">{diffBlock(text, conflictEntry!)}</h2>;
      if (highlightText) return <h2 className="text-2xl font-bold">{highlighted}</h2>;
      return <h2 className="text-2xl font-bold whitespace-pre-wrap">{text}</h2>;

    case "heading_3":
      if (showDiff) return <h3 className="text-xl font-semibold">{diffBlock(text, conflictEntry!)}</h3>;
      if (highlightText) return <h3 className="text-xl font-semibold">{highlighted}</h3>;
      return <h3 className="text-xl font-semibold whitespace-pre-wrap">{text}</h3>;

    case "heading_4":
      if (showDiff) return <h4 className="text-lg font-semibold">{diffBlock(text, conflictEntry!)}</h4>;
      if (highlightText) return <h4 className="text-lg font-semibold">{highlighted}</h4>;
      return <h4 className="text-lg font-semibold whitespace-pre-wrap">{text}</h4>;

    case "quote":
      if (showDiff) return (
        <blockquote className="border-l-4 border-slate-300 pl-4 text-slate-600 italic">
          {diffBlock(text, conflictEntry!)}
        </blockquote>
      );
      if (highlightText) return (
        <blockquote className="border-l-4 border-slate-300 pl-4 text-slate-600 italic">
          {highlighted}
        </blockquote>
      );
      return (
        <blockquote className="border-l-4 border-slate-300 pl-4 text-slate-600 italic whitespace-pre-wrap">
          {text}
        </blockquote>
      );

    case "divider":
      return <hr className="border-slate-200" />;

    case "code":
      if (showDiff) return (
        <pre className="bg-slate-100 rounded-md p-3 overflow-x-auto text-sm font-mono">
          {diffBlock(text, conflictEntry!)}
        </pre>
      );
      return (
        <pre className="bg-slate-100 rounded-md p-3 overflow-x-auto text-sm font-mono whitespace-pre">
          <code>{text}</code>
        </pre>
      );

    case "callout":
      if (showDiff) return (
        <div className="flex items-start gap-2 bg-slate-50 border border-slate-200 rounded-md p-3">
          <span className="flex items-center h-6 shrink-0">
            <Info className="w-4 h-4 text-slate-500" />
          </span>
          <div>{diffBlock(text, conflictEntry!)}</div>
        </div>
      );
      if (highlightText) return (
        <div className="flex items-start gap-2 bg-slate-50 border border-slate-200 rounded-md p-3">
          <span className="flex items-center h-6 shrink-0">
            <Info className="w-4 h-4 text-slate-500" />
          </span>
          <div>{highlighted}</div>
        </div>
      );
      return (
        <div className="flex items-start gap-2 bg-slate-50 border border-slate-200 rounded-md p-3">
          <span className="flex items-center h-6 shrink-0">
            <Info className="w-4 h-4 text-slate-500" />
          </span>
          <div className="whitespace-pre-wrap">{text}</div>
        </div>
      );

    case "to_do":
      if (showDiff) return (
        <div className="flex items-start gap-2">
          <span className="flex items-center h-6 shrink-0">
            <Square className="w-4 h-4 text-slate-400" />
          </span>
          <div>{diffBlock(text, conflictEntry!)}</div>
        </div>
      );
      if (highlightText) return (
        <div className="flex items-start gap-2">
          <span className="flex items-center h-6 shrink-0">
            <Square className="w-4 h-4 text-slate-400" />
          </span>
          <span>{highlighted}</span>
        </div>
      );
      return (
        <div className="flex items-start gap-2">
          <span className="flex items-center h-6 shrink-0">
            <Square className="w-4 h-4 text-slate-400" />
          </span>
          <span className="whitespace-pre-wrap">{text}</span>
        </div>
      );

    default:
      if (showDiff) return <p>{diffBlock(text, conflictEntry!)}</p>;
      if (highlightText) return <p>{highlighted}</p>;
      if (text) return <p className="whitespace-pre-wrap">{text}</p>;
      return (
        <div className="text-sm text-slate-400 border border-dashed border-slate-200 rounded px-2 py-1">
          [{type} 블록 — 지원 예정]
        </div>
      );
  }
}

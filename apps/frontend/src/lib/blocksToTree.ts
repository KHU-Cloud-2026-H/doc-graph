import type { components } from "@docgraph/api-types";

type Block = components["schemas"]["Block"];

export interface BlockTreeNode {
  blockId: string;
  type: string;
  text: string | null;
  order: number;
  children: BlockTreeNode[];
}

/**
 * 평평한 blocks 리스트를 parentBlockId + order 기준으로 트리로 재구성한다.
 * - 누락 필드는 방어적으로 기본값 처리(blockId 자동 생성, order=0, type="paragraph", text=null).
 * - parentBlockId 가 null/undefined 이거나 부모가 목록에 없는 고아 블록은 최상위로 취급(누락 금지).
 * - 모든 형제 배열을 order 오름차순으로 정렬.
 */
export function blocksToTree(blocks: Block[]): BlockTreeNode[] {
  let autoId = 0;

  const entries = blocks.map((block) => {
    const node: BlockTreeNode = {
      blockId: block.blockId ?? `__auto-block-${autoId++}`,
      type: block.type ?? "paragraph",
      text: block.text ?? null,
      order: block.order ?? 0,
      children: [],
    };
    return { node, parentBlockId: block.parentBlockId };
  });

  const byId = new Map<string, BlockTreeNode>();
  for (const { node } of entries) {
    byId.set(node.blockId, node);
  }

  const roots: BlockTreeNode[] = [];
  for (const { node, parentBlockId } of entries) {
    const parent =
      parentBlockId != null ? byId.get(parentBlockId) : undefined;
    if (parent && parent !== node) {
      parent.children.push(node);
    } else {
      roots.push(node);
    }
  }

  const sortRecursive = (list: BlockTreeNode[]) => {
    list.sort((a, b) => a.order - b.order);
    for (const n of list) sortRecursive(n.children);
  };
  sortRecursive(roots);

  return roots;
}

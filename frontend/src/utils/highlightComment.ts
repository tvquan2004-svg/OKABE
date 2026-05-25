type HighlightListener = (commentId: number | null) => void;

let highlightCommentId: number | null = null;

const listeners = new Set<HighlightListener>();

export function setHighlightCommentId(id: number | null) {
  highlightCommentId = id;
  listeners.forEach((listener) => listener(id));
}

export function getHighlightCommentId(): number | null {
  return highlightCommentId;
}

export function onHighlightCommentChange(listener: HighlightListener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}
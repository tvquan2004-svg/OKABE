import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import styles from './AiChatWidget.module.css';

interface MarkdownMessageProps {
  content: string;
}

export function MarkdownMessage({ content }: MarkdownMessageProps) {
  // Trích xuất các action blocks
  const actions: Record<string, unknown>[] = [];
  const actionRegex = /\[ACTION\]([\s\S]*?)\[\/ACTION\]/g;
  let match;
  while ((match = actionRegex.exec(content)) !== null) {
    try {
      const actionObj = JSON.parse(match[1] || '{}');
      actions.push(actionObj);
    } catch (_e) {
      // Bỏ qua lỗi parse JSON khi đang stream dở
    }
  }

  // Ẩn tất cả chuỗi action khỏi tin nhắn (kể cả khi chưa hoàn thành streaming)
  const visibleContent = content.replace(/\[ACTION\][\s\S]*?(?:\[\/ACTION\]|$)/g, '').trim();

  return (
    <div className={styles.mdContainer}>
      {visibleContent && (
        <ReactMarkdown
          remarkPlugins={[remarkGfm]}
          components={{
            h1: ({ ...props }) => <h2 className={styles.mdHeading1} {...props} />,
            h2: ({ ...props }) => <h3 className={styles.mdHeading2} {...props} />,
            h3: ({ ...props }) => <h4 className={styles.mdHeading3} {...props} />,
            p: ({ ...props }) => <p className={styles.mdParagraph} {...props} />,
            ul: ({ ...props }) => <ul className={styles.mdList} {...props} />,
            ol: ({ ...props }) => <ol className={styles.mdListNum} {...props} />,
            li: ({ ...props }) => <li className={styles.mdListItem} {...props} />,
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            code: ({ inline, ...props }: any) => 
              inline ? (
                <code className={styles.mdInlineCode} {...props} />
              ) : (
                <pre className={styles.mdCodeBlock}>
                  <code {...props} />
                </pre>
              )
          }}
        >
          {visibleContent}
        </ReactMarkdown>
      )}

      {/* Hiển thị Action Chips */}
      {actions.length > 0 && (
        <div className={styles.actionContainer}>
          {actions.map((act, i) => (
            <div key={i} className={styles.actionChip}>
              {act.type === 'CREATE_CARD' && (
                <>✨ Đã tự động tạo card <b>{act.title}</b> vào cột <b>{act.listName}</b></>
              )}
              {act.type === 'MOVE_CARD' && (
                <>🚀 Đã tự động kéo card <b>{act.cardTitle}</b> sang cột <b>{act.targetList}</b></>
              )}
              {act.type === 'ASSIGN_MEMBER' && (
                <>👤 Đã giao card <b>{act.cardTitle}</b> cho <b>{act.memberName}</b></>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

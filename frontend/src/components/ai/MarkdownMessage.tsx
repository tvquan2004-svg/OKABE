import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import styles from './AiChatWidget.module.css';

interface MarkdownMessageProps {
  content: string;
}

export function MarkdownMessage({ content }: MarkdownMessageProps) {
  return (
    <div className={styles.mdContainer}>
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
        {content}
      </ReactMarkdown>
    </div>
  );
}

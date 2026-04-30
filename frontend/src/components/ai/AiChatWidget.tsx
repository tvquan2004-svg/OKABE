import React, { useState, useRef, useEffect, useCallback } from 'react';
import { useLocation } from 'react-router-dom';
import { useAiChatStream } from '../../hooks/useAiChatStream';
import { MarkdownMessage } from './MarkdownMessage';
import styles from './AiChatWidget.module.css';

const QUICK_PROMPTS: Record<string, string[]> = {
  dashboard: ['Tóm tắt công việc hôm nay', 'Tasks nào đang quá hạn?', 'Tôi nên ưu tiên gì?'],
  board: ['Tóm tắt board này', 'Ai đang có nhiều tasks nhất?', 'Tasks sắp đến hạn?'],
  default: ['Xem tasks của tôi', 'Tasks quá hạn?', 'Gợi ý công việc hôm nay'],
};

function TypingIndicator() {
  return (
    <div className={styles.typingIndicator}>
      <span /><span /><span />
    </div>
  );
}

export default function AiChatWidget() {
  const [isOpen, setIsOpen] = useState(false);
  const [inputValue, setInputValue] = useState('');
  const [hasNewMessage, setHasNewMessage] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const location = useLocation();

  const { messages, isLoading, sendMessage, clearConversation } = useAiChatStream();

  const getQuickPrompts = useCallback((): string[] => {
    if (location.pathname.includes('/board/')) return QUICK_PROMPTS.board ?? [];
    if (location.pathname.includes('/dashboard')) return QUICK_PROMPTS.dashboard ?? [];
    return QUICK_PROMPTS.default ?? [];
  }, [location.pathname]);

  // Auto-scroll to bottom on new messages
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    if (!isOpen && messages.length > 0) {
      setHasNewMessage(true);
    }
  }, [messages, isOpen]);

  const handleOpen = () => {
    setIsOpen(true);
    setHasNewMessage(false);
    setTimeout(() => inputRef.current?.focus(), 100);
  };

  const handleSend = async () => {
    const text = inputValue.trim();
    if (!text || isLoading) return;
    setInputValue('');
    await sendMessage(text);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleQuickPrompt = (prompt: string) => {
    sendMessage(prompt);
  };

  return (
    <>
      {/* Floating Bubble */}
      <button
        id="ai-chat-bubble"
        className={`${styles.bubble} ${isOpen ? styles.bubbleHidden : ''}`}
        onClick={handleOpen}
        aria-label="Mở OKABE Assistant"
        title="OKABE Assistant"
      >
        <span className={styles.bubbleIcon}>🤖</span>
        {hasNewMessage && <span className={styles.bubbleBadge} />}
      </button>

      {/* Chat Panel */}
      <div
        id="ai-chat-panel"
        className={`${styles.panel} ${isOpen ? styles.panelOpen : ''}`}
        role="dialog"
        aria-label="OKABE Assistant"
      >
        {/* Header */}
        <div className={styles.header}>
          <div className={styles.headerInfo}>
            <div className={styles.avatar}>🤖</div>
            <div>
              <div className={styles.headerTitle}>OKABE Assistant</div>
              <div className={styles.headerStatus}>
                <span className={styles.statusDot} />
                Trực tuyến
              </div>
            </div>
          </div>
          <div className={styles.headerActions}>
            <button
              className={styles.iconBtn}
              onClick={clearConversation}
              title="Cuộc hội thoại mới"
              aria-label="Xoá và bắt đầu lại"
            >
              ✏️
            </button>
            <button
              className={styles.iconBtn}
              onClick={() => setIsOpen(false)}
              aria-label="Đóng chat"
            >
              ✕
            </button>
          </div>
        </div>

        {/* Messages */}
        <div className={styles.messages} role="log" aria-live="polite">
          {messages.length === 0 && (
            <div className={styles.welcome}>
              <div className={styles.welcomeIcon}>👋</div>
              <p className={styles.welcomeText}>
                Xin chào! Tôi là <strong>OKABE Assistant</strong>.
                <br />
                Tôi có thể giúp bạn quản lý công việc thông minh hơn!
              </p>
            </div>
          )}

          {messages.map((msg) => (
            <div
              key={msg.id}
              className={`${styles.message} ${
                msg.role === 'USER' ? styles.messageUser : styles.messageAi
              }`}
            >
              {msg.role === 'ASSISTANT' && (
                <div className={styles.messageAvatar}>🤖</div>
              )}
              <div className={`${styles.bubble2} ${msg.role === 'ASSISTANT' ? styles.bubble2Ai : ''}`}>
                {msg.isLoading && !msg.content ? (
                  <TypingIndicator />
                ) : (
                  <>
                    <MarkdownMessage content={msg.content} />
                    {msg.role === 'ASSISTANT' && !msg.isLoading && (
                      <button 
                        className={styles.copyBtn}
                        onClick={() => navigator.clipboard.writeText(msg.content)}
                        title="Copy"
                      >
                        📋
                      </button>
                    )}
                  </>
                )}
              </div>
            </div>
          ))}

          <div ref={messagesEndRef} />
        </div>

        {/* Quick Prompts — only show when no messages yet */}
        {messages.length === 0 && (
          <div className={styles.quickPrompts}>
            {getQuickPrompts().map((prompt) => (
              <button
                key={prompt}
                className={styles.quickPrompt}
                onClick={() => handleQuickPrompt(prompt)}
                disabled={isLoading}
              >
                {prompt}
              </button>
            ))}
          </div>
        )}

        {/* Input */}
        <div className={styles.inputArea}>
          <textarea
            ref={inputRef}
            id="ai-chat-input"
            className={styles.input}
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Nhập tin nhắn... (Enter để gửi)"
            rows={1}
            disabled={isLoading}
            aria-label="Nhập tin nhắn cho AI"
          />
          <button
            id="ai-chat-send"
            className={styles.sendBtn}
            onClick={handleSend}
            disabled={isLoading || !inputValue.trim()}
            aria-label="Gửi tin nhắn"
          >
            {isLoading ? '⏳' : '➤'}
          </button>
        </div>
      </div>

      {/* Overlay (mobile) */}
      {isOpen && (
        <div
          className={styles.overlay}
          onClick={() => setIsOpen(false)}
          aria-hidden="true"
        />
      )}
    </>
  );
}

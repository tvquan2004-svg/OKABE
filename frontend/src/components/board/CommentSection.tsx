import React, { useState } from 'react';
import {
  useGetCardCommentsQuery,
  useCreateCommentMutation,
  useUpdateCommentMutation,
  useDeleteCommentMutation,
} from '../../services/boardApi';
import { useGetWorkspaceMembersQuery } from '../../services/workspaceApi';
import { MdComment, MdSend, MdEdit, MdDelete } from 'react-icons/md';
import styles from './CardDetailModal.module.css';

interface CommentSectionProps {
  cardId: number;
  workspaceId: number;
}

const CommentSection: React.FC<CommentSectionProps> = ({ cardId, workspaceId }) => {
  const [newComment, setNewComment] = useState('');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editContent, setEditContent] = useState('');

  // Mention state
  const [mentionSearch, setMentionSearch] = useState<string | null>(null);
  const [cursorPos, setCursorPos] = useState(0);
  const [selectedIndex, setSelectedIndex] = useState(0);

  const { data: commentsRes, isLoading } = useGetCardCommentsQuery({ cardId });
  const comments = commentsRes?.data?.content ? [...commentsRes.data.content].reverse() : [];

  const { data: membersRes } = useGetWorkspaceMembersQuery(workspaceId);
  const members = membersRes?.data || [];

  const [createComment] = useCreateCommentMutation();
  const [updateComment] = useUpdateCommentMutation();
  const [deleteComment] = useDeleteCommentMutation();

  const filteredMembers = mentionSearch !== null
    ? members.filter(m => m.username.toLowerCase().includes(mentionSearch.toLowerCase()))
    : [];

  const handleTextChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const value = e.target.value;
    const position = e.target.selectionStart;
    setNewComment(value);
    setCursorPos(position);

    const textBeforeCursor = value.substring(0, position);
    const lastAtIdx = textBeforeCursor.lastIndexOf('@');

    if (lastAtIdx !== -1 && (lastAtIdx === 0 || /\s/.test(textBeforeCursor.charAt(lastAtIdx - 1)))) {
      const search = textBeforeCursor.substring(lastAtIdx + 1);
      if (!/\s/.test(search)) {
        setMentionSearch(search);
        setSelectedIndex(0);
        return;
      }
    }
    setMentionSearch(null);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (mentionSearch !== null && filteredMembers.length > 0) {
      if (e.key === 'ArrowDown') {
        e.preventDefault();
        setSelectedIndex((prev) => (prev + 1) % filteredMembers.length);
      } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        setSelectedIndex((prev) => (prev - 1 + filteredMembers.length) % filteredMembers.length);
      } else if (e.key === 'Enter' || e.key === 'Tab') {
        e.preventDefault();
        if (filteredMembers[selectedIndex]) {
          insertMention(filteredMembers[selectedIndex].username);
        }
      } else if (e.key === 'Escape') {
        setMentionSearch(null);
      }
    }
  };

  const insertMention = (username: string) => {
    const textBeforeAt = newComment.substring(0, newComment.lastIndexOf('@', cursorPos - 1));
    const textAfterCursor = newComment.substring(cursorPos);
    setNewComment(`${textBeforeAt}@${username} ${textAfterCursor}`);
    setMentionSearch(null);
  };

  const handlePostComment = async () => {
    if (!newComment.trim()) return;
    try {
      await createComment({ cardId, content: newComment.trim() }).unwrap();
      setNewComment('');
    } catch (err) {
      console.error('Failed to post comment', err);
    }
  };

  const handleUpdateComment = async (id: number) => {
    if (!editContent.trim()) return;
    try {
      await updateComment({ id, cardId, content: editContent.trim() }).unwrap();
      setEditingId(null);
    } catch (err) {
      console.error('Failed to update comment', err);
    }
  };

  const handleDeleteComment = async (id: number) => {
    if (window.confirm('Delete this comment?')) {
      await deleteComment({ id, cardId }).unwrap();
    }
  };

  const renderContent = (content: string, mentions: any[] = []) => {
    if (!content) return null;
    const mentionUsernames = mentions.map(m => m.username);
    const parts = content.split(/(@[^\s@]+(?:\s+[^\s@]+)*)/g);
    
    return parts.map((part, i) => {
      if (part.startsWith('@')) {
        const username = part.substring(1);
        if (mentionUsernames.includes(username)) {
          return <span key={i} className={styles.mention} style={{ color: '#3b82f6', fontWeight: 600 }}>@{username}</span>;
        }
      }
      return part;
    });
  };

  return (
    <div className={styles.commentSection} style={{ marginTop: '1rem' }}>
      <h3 className={styles.sectionTitle} style={{ marginBottom: '1rem' }}>
        <MdComment /> Comments
      </h3>

      <div className={styles.commentInputWrapper} style={{ display: 'flex', gap: '0.75rem', marginBottom: '2rem', position: 'relative' }}>
        <div className={styles.activityAvatar} style={{ width: '32px', height: '32px' }}>
          U
        </div>
        <div style={{ flex: 1, position: 'relative' }}>
          <textarea
            className={styles.descriptionBox}
            style={{ minHeight: '80px', width: '100%', fontSize: '0.9rem' }}
            placeholder="Write a comment... (use @username to mention)"
            value={newComment}
            onChange={handleTextChange}
            onKeyDown={handleKeyDown}
          />

          {mentionSearch !== null && filteredMembers.length > 0 && (
            <div 
              className={styles.popover} 
              style={{ 
                position: 'absolute', 
                top: 'calc(100% - 40px)', // Đặt ngay dưới textarea
                left: 0, 
                width: '100%', 
                zIndex: 1000,
                background: '#1e293b', // Đảm bảo nền đặc, không trong suốt
                boxShadow: '0 10px 25px rgba(0,0,0,0.5)',
                border: '1px solid #334155'
              }}
            >
              <div className={styles.popoverHeader} style={{ fontSize: '0.7rem', padding: '8px 12px', borderBottom: '1px solid #334155' }}>Gợi ý thành viên</div>
              <div className={styles.popoverBody} style={{ padding: '4px' }}>
                {filteredMembers.map((member, index) => (
                  <div
                    key={member.userId}
                    className={styles.popoverItem}
                    style={{ 
                      background: index === selectedIndex ? 'rgba(59, 130, 246, 0.2)' : 'transparent',
                      padding: '8px 12px',
                      borderRadius: '4px'
                    }}
                    onClick={() => insertMention(member.username)}
                  >
                    <div className={styles.avatarCircleSmall} style={{ width: '24px', height: '24px' }}>
                      {member.avatarUrl ? <img src={member.avatarUrl} alt={member.username} /> : member.username.charAt(0).toUpperCase()}
                    </div>
                    <span style={{ fontSize: '0.85rem', color: '#f1f5f9' }}>{member.username}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          <div style={{ marginTop: '0.75rem' }}>
            <button 
              className={styles.saveBtn} 
              style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.4rem 1rem' }}
              onClick={handlePostComment}
              disabled={!newComment.trim()}
            >
              <MdSend /> Lưu
            </button>
          </div>
        </div>
      </div>

      <div className={styles.activityList}>
        {isLoading ? (
          <div style={{ color: '#64748b', fontSize: '0.9rem' }}>Loading comments...</div>
        ) : (
          comments.map((comment) => (
            <div key={comment.id} className={styles.activityItem} style={{ marginBottom: '1.5rem', alignItems: 'flex-start' }}>
              <div className={styles.activityAvatar} style={{ width: '32px', height: '32px' }}>
                {comment.author.avatarUrl ? (
                  <img src={comment.author.avatarUrl} alt={comment.author.username} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                ) : (
                  comment.author.username.charAt(0).toUpperCase()
                )}
              </div>
              <div className={styles.activityContent} style={{ background: 'rgba(255,255,255,0.03)', padding: '0.75rem', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.05)' }}>
                <div className={styles.activityHeader}>
                  <span className={styles.activityUser} style={{ color: '#f1f5f9', fontWeight: 600 }}>{comment.author.username}</span>
                  <span className={styles.activityTime} style={{ marginLeft: '0.5rem' }}>
                    {new Date(comment.createdAt).toLocaleString()}
                  </span>
                </div>
                
                {editingId === comment.id ? (
                  <div style={{ marginTop: '0.5rem' }}>
                    <textarea
                      className={styles.descriptionBox}
                      style={{ minHeight: '60px', width: '100%', fontSize: '0.9rem' }}
                      value={editContent}
                      onChange={(e) => setEditContent(e.target.value)}
                    />
                    <div style={{ marginTop: '0.5rem', display: 'flex', gap: '0.5rem' }}>
                      <button className={styles.saveBtn} onClick={() => handleUpdateComment(comment.id)}>Cập nhật</button>
                      <button className={styles.cancelBtn} style={{ background: 'transparent', color: '#94a3b8', border: 'none', cursor: 'pointer' }} onClick={() => setEditingId(null)}>Hủy</button>
                    </div>
                  </div>
                ) : (
                  <div style={{ marginTop: '0.25rem', color: '#f1f5f9', lineHeight: 1.5 }}>
                    {renderContent(comment.content as string, comment.mentions)}
                  </div>
                )}

                <div style={{ marginTop: '0.5rem', display: 'flex', gap: '1rem' }}>
                  <button 
                    onClick={() => { setEditingId(comment.id); setEditContent(comment.content as string); }}
                    style={{ background: 'none', border: 'none', color: '#64748b', fontSize: '0.75rem', cursor: 'pointer', padding: 0, display: 'flex', alignItems: 'center', gap: '0.25rem' }}
                  >
                    <MdEdit size={12} /> Sửa
                  </button>
                  <button 
                    onClick={() => handleDeleteComment(comment.id)}
                    style={{ background: 'none', border: 'none', color: '#64748b', fontSize: '0.75rem', cursor: 'pointer', padding: 0, display: 'flex', alignItems: 'center', gap: '0.25rem' }}
                  >
                    <MdDelete size={12} /> Xóa
                  </button>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default CommentSection;

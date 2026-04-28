import React, { useState } from 'react';
import {
  useGetCardCommentsQuery,
  useCreateCommentMutation,
  useUpdateCommentMutation,
  useDeleteCommentMutation,
} from '../../services/boardApi';
import { useGetWorkspaceMembersQuery } from '../../services/workspaceApi';
import { useAppSelector } from '../../hooks/useRedux';
import { MdSend, MdEdit, MdDelete } from 'react-icons/md';
import styles from './CardDetailModal.module.css';

interface CommentSectionProps {
  cardId: number;
  workspaceId: number;
  readOnly?: boolean;
}

const CommentSection: React.FC<CommentSectionProps> = ({ cardId, workspaceId, readOnly = false }) => {
  const [newComment, setNewComment] = useState('');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editContent, setEditContent] = useState('');
  const [showAllComments, setShowAllComments] = useState(false);

  // Mention state
  const [mentionSearch, setMentionSearch] = useState<string | null>(null);
  const [cursorPos, setCursorPos] = useState(0);
  const [selectedIndex, setSelectedIndex] = useState(0);

  const { data: commentsRes, isLoading } = useGetCardCommentsQuery({ cardId });
  const comments = commentsRes?.data?.content ? [...commentsRes.data.content].reverse() : [];

  const { data: membersRes } = useGetWorkspaceMembersQuery(workspaceId);
  const members = membersRes?.data || [];

  const currentUser = useAppSelector(state => state.auth.user);

  const [createComment] = useCreateCommentMutation();
  const [updateComment] = useUpdateCommentMutation();
  const [deleteComment] = useDeleteCommentMutation();

  const filteredMembers = mentionSearch !== null
    ? members.filter(m => 
        m.username.toLowerCase().includes(mentionSearch.toLowerCase()) && 
        m.userId !== currentUser?.id
      )
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
    if (window.confirm('Xóa bình luận này?')) {
      await deleteComment({ id, cardId }).unwrap();
    }
  };

  const renderContent = (content: string, mentions: { username: string }[] = []) => {
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
    <div className={styles.commentSection}>
      {!readOnly && (
        <div className={styles.commentInputWrapper}>
          <div className={styles.activityAvatar}>
            {currentUser?.avatarUrl ? (
              <img src={currentUser.avatarUrl} alt={currentUser.username} />
            ) : (
              currentUser?.username?.charAt(0).toUpperCase() || 'U'
            )}
          </div>
          <div className={styles.inputArea}>
            <textarea
              className={styles.commentTextarea}
              placeholder="Viết bình luận..."
              value={newComment}
              onChange={handleTextChange}
              onKeyDown={handleKeyDown}
            />

            {mentionSearch !== null && filteredMembers.length > 0 && (
              <div className={styles.mentionPopover}>
                {filteredMembers.map((member, index) => (
                  <div
                    key={member.userId}
                    className={`${styles.popoverItem} ${index === selectedIndex ? styles.active : ''}`}
                    onClick={() => insertMention(member.username)}
                  >
                    <div className={styles.avatarCircleSmall}>
                      {member.avatarUrl ? <img src={member.avatarUrl} alt={member.username} /> : member.username.charAt(0).toUpperCase()}
                    </div>
                    <span>{member.username}</span>
                  </div>
                ))}
              </div>
            )}

            <div className={styles.commentActions}>
              <button 
                className={styles.sendBtn}
                onClick={handlePostComment}
                disabled={!newComment.trim()}
              >
                <MdSend /> <span>Gửi</span>
              </button>
            </div>
          </div>
        </div>
      )}

      <div className={styles.commentsList}>
        {isLoading ? (
          <div className={styles.loadingText}>Đang tải bình luận...</div>
        ) : (
          <>
            {(showAllComments ? comments : comments.slice(0, 3)).map((comment) => (
              <div key={comment.id} className={styles.activityItem}>
                <div className={styles.activityAvatar}>
                  {comment.author.avatarUrl ? (
                    <img src={comment.author.avatarUrl} alt={comment.author.username} />
                  ) : (
                    comment.author.username.charAt(0).toUpperCase()
                  )}
                </div>
                <div className={styles.commentBubble}>
                  <div className={styles.activityHeader}>
                    <span className={styles.activityUser}>{comment.author.username}</span>
                    <span className={styles.activityTime}>
                      {new Date(comment.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                  
                  {editingId === comment.id ? (
                    <div className={styles.editCommentBox}>
                      <textarea
                        className={styles.commentTextarea}
                        value={editContent}
                        onChange={(e) => setEditContent(e.target.value)}
                      />
                      <div className={styles.editActions}>
                        <button className={styles.saveBtn} onClick={() => handleUpdateComment(comment.id)}>Lưu</button>
                        <button className={styles.cancelBtn} onClick={() => setEditingId(null)}>Hủy</button>
                      </div>
                    </div>
                  ) : (
                    <div className={styles.commentContentText}>
                      {renderContent(comment.content as string, comment.mentions)}
                    </div>
                  )}

                  {!readOnly && (
                    <div className={styles.commentMetaLinks}>
                      <button onClick={() => { setEditingId(comment.id); setEditContent(comment.content as string); }}><MdEdit size={14} /> Sửa</button>
                      <button onClick={() => handleDeleteComment(comment.id)}><MdDelete size={14} /> Xóa</button>
                    </div>
                  )}
                </div>
              </div>
            ))}
            
            {comments.length > 3 && (
              <button 
                className={styles.showMoreBtn}
                onClick={() => setShowAllComments(!showAllComments)}
                style={{ marginTop: '0.5rem' }}
              >
                {showAllComments ? 'Thu gọn bình luận' : `Xem thêm (${comments.length - 3} bình luận khác)`}
              </button>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default CommentSection;

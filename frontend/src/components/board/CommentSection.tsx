import React, { useState } from 'react';
import {
  useGetCardCommentsQuery,
  useCreateCommentMutation,
  useUpdateCommentMutation,
  useDeleteCommentMutation,
} from '../../services/boardApi';
import { MdComment, MdSend, MdEdit, MdDelete } from 'react-icons/md';
import styles from './CardDetailModal.module.css';

interface CommentSectionProps {
  cardId: number;
}

const CommentSection: React.FC<CommentSectionProps> = ({ cardId }) => {
  const [newComment, setNewComment] = useState('');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editContent, setEditContent] = useState('');

  const { data: commentsRes, isLoading } = useGetCardCommentsQuery({ cardId });
  const comments = commentsRes?.data?.content || [];

  const [createComment] = useCreateCommentMutation();
  const [updateComment] = useUpdateCommentMutation();
  const [deleteComment] = useDeleteCommentMutation();

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

  return (
    <div className={styles.section} style={{ marginTop: '2rem' }}>
      <h3 className={styles.sectionTitle}>
        <MdComment /> Comments
      </h3>

      <div className={styles.commentInputWrapper}>
        <textarea
          className={styles.commentTextarea}
          placeholder="Write a comment... (use @username to mention)"
          value={newComment}
          onChange={(e) => setNewComment(e.target.value)}
        />
        <button 
          className={styles.sendBtn} 
          onClick={handlePostComment}
          disabled={!newComment.trim()}
        >
          <MdSend />
        </button>
      </div>

      <div className={styles.commentList}>
        {isLoading ? (
          <div className={styles.loading}>Loading comments...</div>
        ) : (
          comments.map((comment) => (
            <div key={comment.id} className={styles.commentItem}>
              <div className={styles.commentAvatar}>
                {comment.author.avatarUrl ? (
                  <img src={comment.author.avatarUrl} alt={comment.author.username} />
                ) : (
                  comment.author.username.charAt(0).toUpperCase()
                )}
              </div>
              <div className={styles.commentBody}>
                <div className={styles.commentHeader}>
                  <span className={styles.commentAuthor}>{comment.author.username}</span>
                  <span className={styles.commentTime}>
                    {new Date(comment.createdAt).toLocaleString()}
                    {comment.isEdited && <span className={styles.editedBadge}> (edited)</span>}
                  </span>
                </div>
                
                {editingId === comment.id ? (
                  <div className={styles.editWrapper}>
                    <textarea
                      className={styles.editCommentTextarea}
                      value={editContent}
                      onChange={(e) => setEditContent(e.target.value)}
                    />
                    <div className={styles.editActions}>
                      <button className={styles.saveBtn} onClick={() => handleUpdateComment(comment.id)}>Save</button>
                      <button className={styles.cancelBtn} onClick={() => setEditingId(null)}>Cancel</button>
                    </div>
                  </div>
                ) : (
                  <div className={styles.commentContent}>
                    {renderContentWithMentions(comment.content as string, comment.mentions)}
                  </div>
                )}

                <div className={styles.commentActions}>
                  <button onClick={() => { setEditingId(comment.id); setEditContent(comment.content as string); }}>
                    <MdEdit size={14} /> Edit
                  </button>
                  <button onClick={() => handleDeleteComment(comment.id)}>
                    <MdDelete size={14} /> Delete
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

const renderContentWithMentions = (content: string, mentions: any[]) => {
  if (!content) return null;
  const mentionUsernames = mentions.map(m => m.username);
  const parts = content.split(/(@\w+)/g);
  
  return parts.map((part, i) => {
    if (part.startsWith('@')) {
      const username = part.substring(1);
      if (mentionUsernames.includes(username)) {
        return <span key={i} className={styles.mention}>@{username}</span>;
      }
    }
    return part;
  });
};

export default CommentSection;

import React, { useState, useEffect } from 'react';
import {
  CardItem,
  useUpdateCardMutation,
  useCreateChecklistMutation,
  useCreateChecklistItemMutation,
  useUpdateChecklistItemMutation,
  useCreateLabelMutation,
  useAddLabelToCardMutation,
  useRemoveLabelFromCardMutation,
  useGetBoardLabelsQuery,
  useAssignMemberMutation,
  useUnassignMemberMutation,
  useUploadAttachmentMutation,
  useDeleteAttachmentMutation,
  useGetCardActivitiesQuery,
} from '../../services/boardApi';
import {
  MdAttachFile,
  MdDelete,
  MdCloudUpload,
  MdInsertDriveFile,
  MdList,
} from 'react-icons/md';
import {
  FaRegFilePdf,
  FaRegFileWord,
  FaRegFileImage,
  FaRegFileArchive,
} from 'react-icons/fa';
import {
  useGetWorkspaceMembersQuery,
} from '../../services/workspaceApi';
import styles from './CardDetailModal.module.css';

interface CardDetailModalProps {
  card: CardItem;
  boardId: number;
  workspaceId: number;
  onClose: () => void;
  priorityColor: (priority: string) => string;
}

const PRESET_COLORS = [
  '#22c55e', '#3b82f6', '#f59e0b', '#ef4444', '#a855f7', 
  '#ec4899', '#06b6d4', '#64748b'
];

const CardDetailModal: React.FC<CardDetailModalProps> = ({
  card,
  boardId,
  workspaceId,
  onClose,
  priorityColor,
}) => {
  const [title, setTitle] = useState(card.title);
  const [description, setDescription] = useState(card.description || '');
  const [newItemContent, setNewItemContent] = useState<{ [key: number]: string }>({});
  const [showMemberPicker, setShowMemberPicker] = useState(false);

  const [updateCard] = useUpdateCardMutation();
  const [createChecklist] = useCreateChecklistMutation();
  const [createChecklistItem] = useCreateChecklistItemMutation();
  const [updateChecklistItem] = useUpdateChecklistItemMutation();
  const [createLabel] = useCreateLabelMutation();
  const [addLabelToCard] = useAddLabelToCardMutation();
  const [removeLabelFromCard] = useRemoveLabelFromCardMutation();
  const [assignMember] = useAssignMemberMutation();
  const [unassignMember] = useUnassignMemberMutation();
  const [uploadAttachment] = useUploadAttachmentMutation();
  const [deleteAttachment] = useDeleteAttachmentMutation();
  const { data: activitiesRes } = useGetCardActivitiesQuery(card.id);
  const activities = activitiesRes?.data || [];
  
  const { data: labelsData } = useGetBoardLabelsQuery(boardId);
  const boardLabels = labelsData?.data || [];

  const { data: workspaceMembersData } = useGetWorkspaceMembersQuery(workspaceId);
  const workspaceMembers = workspaceMembersData?.data || [];

  useEffect(() => {
    setTitle(card.title);
    setDescription(card.description || '');
  }, [card]);

  const handleUpdateCard = async (body: Partial<CardItem>) => {
    await updateCard({ id: card.id, boardId, body }).unwrap();
  };

  const handleCreateChecklist = async () => {
    const name = prompt('Enter checklist name:');
    if (name?.trim()) {
      await createChecklist({ cardId: card.id, boardId, name: name.trim() }).unwrap();
    }
  };

  const handleAddItem = async (checklistId: number) => {
    const content = newItemContent[checklistId];
    if (content?.trim()) {
      await createChecklistItem({ checklistId, boardId, cardId: card.id, content: content.trim() }).unwrap();
      setNewItemContent({ ...newItemContent, [checklistId]: '' });
    }
  };

  const handleToggleItem = async (itemId: number, isCompleted: boolean) => {
    await updateChecklistItem({ itemId, boardId, cardId: card.id, body: { isCompleted } }).unwrap();
  };

  const handleAddLabel = async (labelId: number) => {
    if (!card.labels.some(l => l.id === labelId)) {
      await addLabelToCard({ cardId: card.id, labelId, boardId }).unwrap();
    }
  };

  const handleRemoveLabel = async (labelId: number) => {
    await removeLabelFromCard({ cardId: card.id, labelId, boardId }).unwrap();
  };

  const handleCreateAndAddLabel = async (color: string) => {
    const name = prompt('Enter label name (optional):');
    const res = await createLabel({ boardId, color, name: name || '' }).unwrap();
    if (res.success) {
      await addLabelToCard({ cardId: card.id, labelId: res.data.id, boardId }).unwrap();
    }
  };

  const handleAssignMember = async (userId: number) => {
    if (!card.members.some(m => m.id === userId)) {
      await assignMember({ cardId: card.id, userId, boardId }).unwrap();
    }
    setShowMemberPicker(false);
  };

  const handleUnassignMember = async (userId: number) => {
    await unassignMember({ cardId: card.id, userId, boardId }).unwrap();
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      try {
        await uploadAttachment({ cardId: card.id, boardId, file }).unwrap();
      } catch (err) {
        alert('Failed to upload file');
      }
    }
  };

  const handleDeleteAttachment = async (attachmentId: number) => {
    if (window.confirm('Are you sure you want to delete this attachment?')) {
      await deleteAttachment({ attachmentId, boardId, cardId: card.id }).unwrap();
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const getFileIcon = (mimeType: string) => {
    if (mimeType.includes('image')) return <FaRegFileImage />;
    if (mimeType.includes('pdf')) return <FaRegFilePdf />;
    if (mimeType.includes('word') || mimeType.includes('officedocument.wordprocessingml')) return <FaRegFileWord />;
    if (mimeType.includes('zip') || mimeType.includes('rar') || mimeType.includes('archive')) return <FaRegFileArchive />;
    return <MdInsertDriveFile />;
  };

  return (
    <div className={styles.overlay} onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className={styles.modal}>
        <header className={styles.header}>
          <div className={styles.titleWrapper}>
            <input
              className={styles.titleInput}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              onBlur={() => title !== card.title && handleUpdateCard({ title })}
            />
            <div style={{ fontSize: '0.85rem', color: '#64748b', marginTop: '0.25rem' }}>
              in list <span style={{ textDecoration: 'underline' }}>TaskList</span>
            </div>
          </div>
          <button className={styles.closeBtn} onClick={onClose}>&times;</button>
        </header>

        <div className={styles.body}>
          <main className={styles.mainContent}>
            {/* Top Info Bar (Labels & Members) */}
            <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap', marginBottom: '1.5rem' }}>
              {card.labels.length > 0 && (
                <div className={styles.section} style={{ flex: 1, minWidth: '150px' }}>
                  <h3 className={styles.sidebarLabel}>Labels</h3>
                  <div className={styles.labelsList}>
                    {card.labels.map(label => (
                      <div
                        key={label.id}
                        className={styles.labelItem}
                        style={{ background: label.color }}
                        onClick={() => handleRemoveLabel(label.id)}
                        title="Click to remove"
                      >
                        {label.name}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {card.members.length > 0 && (
                <div className={styles.section} style={{ flex: 1, minWidth: '150px' }}>
                  <h3 className={styles.sidebarLabel}>Members</h3>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
                    {card.members.map(member => (
                      <div 
                        key={member.id} 
                        className={styles.avatarCircle} 
                        title={`${member.username} (Click to unassign)`}
                        onClick={() => handleUnassignMember(member.id)}
                      >
                        {member.avatarUrl ? (
                          <img src={member.avatarUrl} alt={member.username} className={styles.avatarImg} />
                        ) : (
                          member.username.charAt(0).toUpperCase()
                        )}
                      </div>
                    ))}
                    <button className={styles.addAvatarBtn} onClick={() => setShowMemberPicker(true)}>+</button>
                  </div>
                </div>
              )}
            </div>

            {/* Description Section */}
            <div className={styles.section}>
              <h3 className={styles.sectionTitle}>
                <span>📝</span> Description
              </h3>
              <textarea
                className={styles.descriptionBox}
                placeholder="Add a more detailed description..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                onBlur={() => description !== (card.description || '') && handleUpdateCard({ description })}
              />
            </div>

            {/* Checklists Section */}
            {card.checklists.map(checklist => {
              const completedCount = checklist.items.filter(i => i.isCompleted).length;
              const percent = checklist.items.length > 0 
                ? Math.round((completedCount / checklist.items.length) * 100) 
                : 0;
              
              return (
                <div key={checklist.id} className={styles.checklist}>
                  <div className={styles.checklistHeader}>
                    <h3 className={styles.sectionTitle}>
                      <span>✅</span> {checklist.name}
                    </h3>
                  </div>
                  
                  <div className={styles.progressBar}>
                    <span className={styles.progressPercent}>{percent}%</span>
                    <div className={styles.progressTrack}>
                      <div className={styles.progressFill} style={{ width: `${percent}%` }} />
                    </div>
                  </div>

                  <div className={styles.itemsList}>
                    {checklist.items.map(item => (
                      <div key={item.id} className={styles.checklistItem}>
                        <input
                          type="checkbox"
                          className={styles.checkbox}
                          checked={item.isCompleted}
                          onChange={(e) => handleToggleItem(item.id, e.target.checked)}
                        />
                        <span className={`${styles.itemContent} ${item.isCompleted ? styles.itemCompleted : ''}`}>
                          {item.content}
                        </span>
                      </div>
                    ))}
                    <input
                      className={styles.addItemInput}
                      placeholder="Add an item..."
                      value={newItemContent[checklist.id] || ''}
                      onChange={(e) => setNewItemContent({ ...newItemContent, [checklist.id]: e.target.value })}
                      onKeyDown={(e) => e.key === 'Enter' && handleAddItem(checklist.id)}
                    />
                  </div>
                </div>
              );
            })}
            {/* Attachments Section */}
            <div className={styles.section} style={{ marginTop: '1rem' }}>
              <h3 className={styles.sectionTitle}>
                <MdAttachFile /> Attachments
              </h3>
              <div className={styles.attachmentsList}>
                {card.attachments?.map((attachment) => (
                  <div key={attachment.id} className={styles.attachmentItem}>
                    <div className={styles.fileThumbnail}>
                      {attachment.mimeType.includes('image') ? (
                        <img src={attachment.url} alt={attachment.filename} className={styles.thumbnailImg} />
                      ) : (
                        <div style={{ fontSize: '1.5rem' }}>{getFileIcon(attachment.mimeType)}</div>
                      )}
                    </div>
                    <div className={styles.fileInfo}>
                      <a href={attachment.url} target="_blank" rel="noopener noreferrer" className={styles.fileName}>
                        {attachment.filename}
                      </a>
                      <div className={styles.fileMeta}>
                        {formatFileSize(attachment.fileSize)} • Added {new Date(attachment.createdAt).toLocaleDateString()}
                      </div>
                    </div>
                    <button 
                      className={styles.deleteFileBtn}
                      onClick={() => handleDeleteAttachment(attachment.id)}
                      title="Delete attachment"
                    >
                      <MdDelete size={18} />
                    </button>
                  </div>
                ))}

                <div className={styles.uploadZone}>
                  <label className={styles.uploadLabel}>
                    <MdCloudUpload size={20} />
                    <span>Upload a file...</span>
                    <input 
                      type="file" 
                      className={styles.fileInput} 
                      onChange={handleFileUpload}
                    />
                  </label>
                </div>
              </div>
            </div>
            {/* Activity Section */}
            <div className={styles.section} style={{ marginTop: '2rem' }}>
              <h3 className={styles.sectionTitle}>
                <MdList /> Activity
              </h3>
              <div className={styles.activityList}>
                {activities.map((activity) => (
                  <div key={activity.id} className={styles.activityItem}>
                    <div className={styles.activityAvatar}>
                      {activity.avatarUrl ? (
                        <img src={activity.avatarUrl} alt={activity.username} />
                      ) : (
                        activity.username.charAt(0).toUpperCase()
                      )}
                    </div>
                    <div className={styles.activityContent}>
                      <div className={styles.activityHeader}>
                        <span className={styles.activityUser}>{activity.username}</span>
                        <span className={styles.activityAction}>
                          {activity.description || activity.actionType.toLowerCase().replace('_', ' ')}
                        </span>
                      </div>
                      <span className={styles.activityTime}>
                        {new Date(activity.createdAt).toLocaleString()}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </main>

          <aside className={styles.sidebar}>
            <div className={styles.sidebarGroup}>
              <h3 className={styles.sidebarLabel}>Add to card</h3>
              <div style={{ position: 'relative' }}>
                <button className={styles.actionBtn} onClick={() => setShowMemberPicker(!showMemberPicker)}>
                  <span>👤</span> Members
                </button>
                {showMemberPicker && (
                  <div className={styles.popover}>
                    <div className={styles.popoverHeader}>
                      <span>Members</span>
                      <button onClick={() => setShowMemberPicker(false)}>&times;</button>
                    </div>
                    <div className={styles.popoverBody}>
                      {workspaceMembers.map(m => (
                        <div 
                          key={m.userId} 
                          className={styles.popoverItem}
                          onClick={() => handleAssignMember(m.userId)}
                        >
                          <div className={styles.avatarCircleSmall}>
                            {m.avatarUrl ? <img src={m.avatarUrl} alt={m.username} /> : m.username.charAt(0).toUpperCase()}
                          </div>
                          <span>{m.username}</span>
                          {card.members.some(cm => cm.id === m.userId) && <span style={{ marginLeft: 'auto' }}>✔</span>}
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
              <button className={styles.actionBtn} onClick={handleCreateChecklist}>
                <span>✅</span> Checklist
              </button>
            </div>

            <div className={styles.sidebarGroup}>
              <h3 className={styles.sidebarLabel}>Labels (Quick Add)</h3>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
                {PRESET_COLORS.map(color => (
                  <div
                    key={color}
                    style={{ width: '32px', height: '24px', background: color, borderRadius: '4px', cursor: 'pointer' }}
                    onClick={() => handleCreateAndAddLabel(color)}
                  />
                ))}
              </div>
              {boardLabels.length > 0 && (
                <div style={{ marginTop: '8px' }}>
                   <h3 className={styles.sidebarLabel} style={{ fontSize: '0.65rem' }}>Board Labels</h3>
                   <div className={styles.labelsList}>
                    {boardLabels.map(l => (
                      <div 
                        key={l.id} 
                        style={{ width: '100%', height: '8px', background: l.color, borderRadius: '2px', cursor: 'pointer' }}
                        onClick={() => handleAddLabel(l.id)}
                        title={l.name}
                      />
                    ))}
                   </div>
                </div>
              )}
            </div>

            <div className={styles.sidebarGroup}>
              <h3 className={styles.sidebarLabel}>Priority</h3>
              <select
                className={styles.prioritySelect}
                value={card.priority}
                onChange={(e) => handleUpdateCard({ priority: e.target.value })}
                style={{ borderLeft: `4px solid ${priorityColor(card.priority)}` }}
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="CRITICAL">Critical</option>
              </select>
            </div>

            <div className={styles.sidebarGroup}>
              <h3 className={styles.sidebarLabel}>Due Date</h3>
              <input
                type="datetime-local"
                className={styles.datePicker}
                value={card.dueDate ? card.dueDate.slice(0, 16) : ''}
                onChange={(e) => handleUpdateCard({ dueDate: e.target.value })}
              />
            </div>
          </aside>
        </div>
      </div>
    </div>
  );
};

export default CardDetailModal;

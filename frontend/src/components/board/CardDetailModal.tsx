import React, { useState, useEffect } from 'react';
import {
  CardItem,
  useUpdateCardMutation,
  useCreateChecklistMutation,
  useUpdateChecklistMutation,
  useDeleteChecklistMutation,
  useCreateChecklistItemMutation,
  useUpdateChecklistItemMutation,
  useDeleteChecklistItemMutation,
  useCreateLabelMutation,
  useAddLabelToCardMutation,
  useRemoveLabelFromCardMutation,
  useGetBoardLabelsQuery,
  useAssignMemberMutation,
  useUnassignMemberMutation,
  useUploadAttachmentMutation,
  useDeleteAttachmentMutation,
  useGetCardActivitiesQuery,
  useArchiveCardMutation,
} from '../../services/boardApi';
import { getFullFileUrl } from '../../utils/urlHelper';
import {
  MdAttachFile,
  MdDelete,
  MdInsertDriveFile,
  MdList,
  MdEdit,
  MdOutlineDescription,
  MdCheckBox,
  MdClose,
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
import { useBreakdownTaskMutation, useSuggestPriorityMutation } from '../../services/aiApi';
import type { PrioritySuggestion } from '../../types/ai.types';
import { FiArchive, FiCheckSquare, FiPaperclip, FiTag, FiClock, FiCalendar, FiUsers } from 'react-icons/fi';
import CommentSection from './CommentSection';
import styles from './CardDetailModal.module.css';

interface CardDetailModalProps {
  card: CardItem;
  boardId: number;
  workspaceId: number;
  onClose: () => void;
  priorityColor: (priority: string) => string;
  readOnly?: boolean;
  highlightCommentId?: number;
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
  readOnly = false,
  highlightCommentId,
}) => {
  const [title, setTitle] = useState(card.title);
  const [description, setDescription] = useState(card.description || '');
  const [startDate, setStartDate] = useState(card.startDate ? card.startDate.slice(0, 16) : '');
  const [dueDate, setDueDate] = useState(card.dueDate ? card.dueDate.slice(0, 16) : '');
  const [newItemContent, setNewItemContent] = useState<{ [key: number]: string }>({});
  const [editingChecklistId, setEditingChecklistId] = useState<number | null>(null);
  const [editingChecklistName, setEditingChecklistName] = useState('');
  const [showMemberPicker, setShowMemberPicker] = useState(false);
  const [showLabelPicker, setShowLabelPicker] = useState(false);
  const [showDatePicker, setShowDatePicker] = useState(false);
  const [showAllActivities, setShowAllActivities] = useState(false);
  const [memberSearch, setMemberSearch] = useState('');
  const fileInputRef = React.useRef<HTMLInputElement>(null);

  const [updateCard] = useUpdateCardMutation();
  const [createChecklist] = useCreateChecklistMutation();
  const [updateChecklist] = useUpdateChecklistMutation();
  const [deleteChecklist] = useDeleteChecklistMutation();
  const [createChecklistItem] = useCreateChecklistItemMutation();
  const [updateChecklistItem] = useUpdateChecklistItemMutation();
  const [deleteChecklistItem] = useDeleteChecklistItemMutation();
  const [createLabel] = useCreateLabelMutation();
  const [addLabelToCard] = useAddLabelToCardMutation();
  const [removeLabelFromCard] = useRemoveLabelFromCardMutation();
  const [assignMember] = useAssignMemberMutation();
  const [unassignMember] = useUnassignMemberMutation();
  const [uploadAttachment] = useUploadAttachmentMutation();
  const [deleteAttachment] = useDeleteAttachmentMutation();
  const [archiveCard] = useArchiveCardMutation();
  const [breakdownTask] = useBreakdownTaskMutation();
  const [suggestPriority] = useSuggestPriorityMutation();

  const [aiPrioritySuggestion, setAiPrioritySuggestion] = useState<PrioritySuggestion | null>(null);
  const [aiPriorityLoading, setAiPriorityLoading] = useState(false);
  
  const { data: activitiesRes } = useGetCardActivitiesQuery(card.id);
  const activities = activitiesRes?.data || [];
  
  const { data: labelsData } = useGetBoardLabelsQuery(boardId);
  const boardLabels = labelsData?.data || [];

  const { data: workspaceMembersData } = useGetWorkspaceMembersQuery(workspaceId);
  const workspaceMembers = workspaceMembersData?.data || [];

  useEffect(() => {
    setTitle(card.title);
    setDescription(card.description || '');
    setStartDate(card.startDate ? card.startDate.slice(0, 16) : '');
    setDueDate(card.dueDate ? card.dueDate.slice(0, 16) : '');
  }, [card]);

  useEffect(() => {
    let cancelled = false;
    setAiPriorityLoading(true);
    suggestPriority({ cardId: card.id })
      .unwrap()
      .then(res => {
        if (cancelled) return;
        const suggestion = res?.data;
        if (!suggestion) return;
        setAiPrioritySuggestion(suggestion);
        const isDefaultPriority = !card.priority || card.priority === 'MEDIUM';
        if (isDefaultPriority && suggestion.suggestedPriority !== 'MEDIUM') {
          handleUpdateCard({ priority: suggestion.suggestedPriority });
        }
      })
      .catch(() => {
        if (!cancelled) setAiPrioritySuggestion(null);
      })
      .finally(() => {
        if (!cancelled) setAiPriorityLoading(false);
      });
    return () => { cancelled = true; };
  }, [card.id]);

  const handleUpdateCard = async (body: Partial<CardItem>) => {
    await updateCard({ id: card.id, boardId, body }).unwrap();
  };

  const handleCreateChecklist = async () => {
    const name = prompt('Nhập tên danh sách kiểm tra:');
    if (name?.trim()) {
      await createChecklist({ cardId: card.id, boardId, name: name.trim() }).unwrap();
    }
  };

  const handleUpdateChecklist = async (checklistId: number) => {
    if (editingChecklistName.trim()) {
      await updateChecklist({ checklistId, boardId, cardId: card.id, name: editingChecklistName.trim() }).unwrap();
      setEditingChecklistId(null);
    }
  };

  const handleDeleteChecklist = async (checklistId: number) => {
    if (confirm('Xóa hoàn toàn danh sách kiểm tra này?')) {
      await deleteChecklist({ checklistId, boardId, cardId: card.id }).unwrap();
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
    await updateChecklistItem({ itemId, boardId, cardId: card.id, isCompleted }).unwrap();
  };

  const handleDeleteItem = async (itemId: number) => {
    await deleteChecklistItem({ itemId, boardId, cardId: card.id }).unwrap();
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
    const name = prompt('Nhập tên nhãn (không bắt buộc):');
    if (name === null) return;
    const res = await createLabel({ boardId, color, name: name.trim() || '' }).unwrap();
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

  const handleArchiveCard = async () => {
    if (confirm('Lưu trữ thẻ này?')) {
      await archiveCard({ id: card.id, boardId }).unwrap();
      onClose();
    }
  };

  const handleAiBreakdown = async () => {
    if (!confirm('🧠 Tự động phân rã task này thành các subtask bằng AI?')) return;
    try {
      const res = await breakdownTask({ cardId: card.id }).unwrap();
      const suggestions = res?.data;
      if (!suggestions || suggestions.length === 0) {
        alert('AI không thể phân rã task này. Vui lòng thử lại.');
        return;
      }
      const checklistRes = await createChecklist({
        cardId: card.id,
        boardId,
        name: '🧠 Phân rã tự động',
      }).unwrap();
      const checklistId = checklistRes.data.id;
      await Promise.all(
        suggestions.map(s =>
          createChecklistItem({
            checklistId,
            boardId,
            cardId: card.id,
            content: `${s.title} (${s.estimatedHours}h)`,
          }).unwrap()
        )
      );
    } catch (err: unknown) {
      const error = err as { data?: { message?: string }, message?: string };
      alert(`Phân rã thất bại: ${error?.data?.message || error?.message || 'Lỗi kết nối'}`);
    }
  };

  const getFullUrl = getFullFileUrl;

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      try {
        await uploadAttachment({ cardId: card.id, boardId, file }).unwrap();
        if (fileInputRef.current) fileInputRef.current.value = '';
      } catch (err: unknown) {
        const error = err as { data?: { message?: string }, message?: string };
        alert(`Tải lên thất bại: ${error?.data?.message || error?.message || 'Lỗi kết nối'}`);
      }
    }
  };

  const handleDeleteAttachment = async (attachmentId: number) => {
    if (window.confirm('Bạn có chắc chắn muốn xóa tệp đính kèm này?')) {
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

  const firstImage = card.attachments?.find(a => a.mimeType.includes('image'));

  return (
    <div className={styles.overlay} onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className={styles.modal}>
        {firstImage && (
          <div className={styles.coverImage}>
            <img src={getFullUrl(firstImage.url)} alt="Cover" />
          </div>
        )}
        
        <header className={styles.header}>
          <div className={styles.titleWrapper}>
            <div className={styles.titleRow}>
              <MdCheckBox className={styles.headerIcon} />
              {readOnly ? (
                <h2 className={styles.titleDisplay}>{title}</h2>
              ) : (
                <input
                  className={styles.titleInput}
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  onBlur={() => title !== card.title && handleUpdateCard({ title })}
                />
              )}
            </div>
            <p className={styles.subtitle}>trong danh sách công việc</p>
          </div>
          <button className={styles.closeBtn} onClick={onClose}><MdClose /></button>
        </header>

        {!readOnly && (
          <div className={styles.quickActions}>
            <button className={styles.quickActionBtn} onClick={handleCreateChecklist}>
              <FiCheckSquare /> <span>Việc cần làm</span>
            </button>
            <button className={styles.quickActionBtn} onClick={handleAiBreakdown}>
              🧠 <span>Phân rã</span>
            </button>
            <button className={styles.quickActionBtn} onClick={() => fileInputRef.current?.click()}>
              <FiPaperclip /> <span>Đính kèm</span>
            </button>
            <button className={styles.quickActionBtn} onClick={() => setShowLabelPicker(!showLabelPicker)}>
              <FiTag /> <span>Nhãn</span>
            </button>
            <button className={styles.quickActionBtn} onClick={() => setShowDatePicker(!showDatePicker)}>
              <FiClock /> <span>Ngày</span>
            </button>
            <button className={styles.quickActionBtn} onClick={() => setShowMemberPicker(!showMemberPicker)}>
              <FiUsers /> <span>Thành viên</span>
            </button>
            <button className={styles.quickActionBtn} onClick={handleArchiveCard}>
              <FiArchive /> <span>Lưu trữ</span>
            </button>
          </div>
        )}

        <div className={styles.contentGrid}>
          <main className={styles.mainContent}>
            {/* Metadata Section (Members, Labels, Priority, Due Date) */}
            <div className={styles.metadataRow}>
              <div className={styles.metaSection}>
                <h4 className={styles.metaLabel}>Độ ưu tiên</h4>
                <div className={styles.priorityDisplay}>
                  <span
                    className={styles.priorityBadge}
                    style={{ background: priorityColor(card.priority) }}
                  >
                    {card.priority}
                  </span>
                  {aiPrioritySuggestion && aiPrioritySuggestion.suggestedPriority !== card.priority && (
                    <span
                      className={styles.aiPriorityBadge}
                      title={`AI gợi ý: ${aiPrioritySuggestion.reason}`}
                      onClick={() => handleUpdateCard({ priority: aiPrioritySuggestion.suggestedPriority })}
                    >
                      AI: {aiPrioritySuggestion.suggestedPriority}
                    </span>
                  )}
                  {aiPriorityLoading && <span className={styles.aiPriorityLoading}>...</span>}
                </div>
              </div>

              <div className={styles.metaSection}>
                <h4 className={styles.metaLabel}>Thành viên</h4>
                <div className={styles.avatarGroup}>
                  {card.members.map(member => (
                    <div 
                      key={member.id} 
                      className={styles.avatarCircle} 
                      title={`${member.username} (click để gỡ)`}
                      onClick={() => !readOnly && handleUnassignMember(member.id)}
                    >
                      {member.avatarUrl ? (
                        <img src={member.avatarUrl} alt={member.username} />
                      ) : (
                        member.username.charAt(0).toUpperCase()
                      )}
                    </div>
                  ))}
                  {!readOnly && (
                    <button
                      className={styles.addMetaBtn}
                      onClick={() => setShowMemberPicker(true)}
                      title="Thêm thành viên"
                    >
                      +
                    </button>
                  )}
                </div>
              </div>

              {card.labels.length > 0 && (
                <div className={styles.metaSection}>
                  <h4 className={styles.metaLabel}>Nhãn</h4>
                  <div className={styles.labelsList}>
                    {card.labels.map(label => (
                      <div
                        key={label.id}
                        className={styles.labelChip}
                        style={{ background: label.color }}
                        onClick={() => !readOnly && handleRemoveLabel(label.id)}
                      >
                        {label.name}
                      </div>
                    ))}
                    {!readOnly && <button className={styles.addMetaBtn} onClick={() => setShowLabelPicker(true)}>+</button>}
                  </div>
                </div>
              )}

              <div className={styles.metaSection}>
                <h4 className={styles.metaLabel}>Ngày</h4>
                <div 
                  className={`${styles.dueDateChip} ${dueDate && new Date(dueDate) < new Date() ? styles.overdue : ''}`}
                  onClick={() => !readOnly && setShowDatePicker(!showDatePicker)}
                >
                  {dueDate ? (
                    <>
                      {startDate && `${new Date(startDate).toLocaleDateString()} - `}
                      {new Date(dueDate).toLocaleString('vi-VN', { 
                        day: '2-digit', month: '2-digit', year: 'numeric', 
                        hour: '2-digit', minute: '2-digit' 
                      })}
                      {new Date(dueDate) < new Date() ? <span className={styles.badge}>Quá hạn</span> : ''}
                    </>
                  ) : 'Chưa thiết lập ngày'}
                </div>
              </div>
            </div>

            {/* Description Section */}
            <div className={styles.section}>
              <div className={styles.sectionHeader}>
                <MdOutlineDescription className={styles.sectionIcon} />
                <h3 className={styles.sectionTitle}>Mô tả</h3>
              </div>
              {readOnly ? (
                <div className={styles.descriptionDisplay}>{description || 'Không có mô tả'}</div>
              ) : (
                <textarea
                  className={styles.descriptionBox}
                  placeholder="Thêm mô tả chi tiết hơn..."
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  onBlur={() => description !== (card.description || '') && handleUpdateCard({ description })}
                />
              )}
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
                    {editingChecklistId === checklist.id ? (
                      <input
                        className={styles.editChecklistInput}
                        value={editingChecklistName}
                        onChange={(e) => setEditingChecklistName(e.target.value)}
                        autoFocus
                        onBlur={() => handleUpdateChecklist(checklist.id)}
                      />
                    ) : (
                      <>
                        <div className={styles.checklistTitleRow}>
                          <MdCheckBox className={styles.sectionIcon} />
                          <h3 className={styles.sectionTitle}>{checklist.name}</h3>
                        </div>
                        {!readOnly && (
                          <div className={styles.checklistActions}>
                            <button onClick={() => { setEditingChecklistId(checklist.id); setEditingChecklistName(checklist.name); }}><MdEdit /></button>
                            <button onClick={() => handleDeleteChecklist(checklist.id)}><MdDelete /></button>
                          </div>
                        )}
                      </>
                    )}
                  </div>
                  
                  <div className={styles.progressContainer}>
                    <span className={styles.percentText}>{percent}%</span>
                    <div className={styles.progressTrack}>
                      <div className={styles.progressFill} style={{ width: `${percent}%`, background: percent === 100 ? '#22c55e' : '#3b82f6' }} />
                    </div>
                  </div>

                  <div className={styles.itemsList}>
                    {checklist.items.map(item => (
                      <div key={item.id} className={styles.checklistItem}>
                        <input
                          type="checkbox"
                          className={styles.checkbox}
                          checked={item.isCompleted}
                          onChange={(e) => !readOnly && handleToggleItem(item.id, e.target.checked)}
                          disabled={readOnly}
                        />
                        <span className={`${styles.itemContent} ${item.isCompleted ? styles.itemCompleted : ''}`}>
                          {item.content}
                        </span>
                        {!readOnly && <button className={styles.deleteItemBtn} onClick={() => handleDeleteItem(item.id)}><MdDelete /></button>}
                      </div>
                    ))}
                    {!readOnly && (
                      <input
                        className={styles.addItemInput}
                        placeholder="Thêm một mục..."
                        value={newItemContent[checklist.id] || ''}
                        onChange={(e) => setNewItemContent({ ...newItemContent, [checklist.id]: e.target.value })}
                        onKeyDown={(e) => e.key === 'Enter' && handleAddItem(checklist.id)}
                      />
                    )}
                  </div>
                </div>
              );
            })}

            {/* Attachments Section */}
            <div className={styles.section}>
              <div className={styles.sectionHeader}>
                <MdAttachFile className={styles.sectionIcon} />
                <h3 className={styles.sectionTitle}>Các tập tin đính kèm</h3>
              </div>
              <div className={styles.attachmentsList}>
                {card.attachments?.map((attachment) => (
                  <div key={attachment.id} className={styles.attachmentCard}>
                    <div className={styles.attachmentThumb}>
                      {attachment.mimeType.includes('image') ? (
                        <img src={getFullUrl(attachment.url)} alt={attachment.filename} />
                      ) : (
                        <div className={styles.filePlaceholder}>{getFileIcon(attachment.mimeType)}</div>
                      )}
                    </div>
                    <div className={styles.attachmentInfo}>
                      <a href={attachment.url} target="_blank" rel="noopener noreferrer" className={styles.attachmentName}>
                        {attachment.filename}
                      </a>
                      <p className={styles.attachmentMeta}>
                        Đã thêm {new Date(attachment.createdAt).toLocaleDateString()} • {formatFileSize(attachment.fileSize)}
                      </p>
                      {!readOnly && (
                        <div className={styles.attachmentLinks}>
                          <button onClick={() => handleDeleteAttachment(attachment.id)}>Xóa</button>
                          <span>•</span>
                          <button>Chỉnh sửa</button>
                        </div>
                      )}
                    </div>
                  </div>
                ))}
                {!readOnly && (
                  <button className={styles.addAttachmentBtn} onClick={() => fileInputRef.current?.click()}>
                    Thêm đính kèm
                  </button>
                )}
              </div>
            </div>
          </main>

          <aside className={styles.activityColumn}>
            <div className={styles.activityHeaderRow}>
              <MdList className={styles.sectionIcon} />
              <h3 className={styles.sectionTitle}>Nhận xét và hoạt động</h3>
            </div>
            
            <CommentSection cardId={card.id} workspaceId={workspaceId} readOnly={readOnly} highlightCommentId={highlightCommentId} />

            <div className={styles.activityList}>
              {(showAllActivities ? activities : activities.slice(0, 5)).map((activity) => (
                <div key={activity.id} className={styles.activityItem}>
                  <div className={styles.activityAvatar}>
                    {activity.avatarUrl ? (
                      <img src={activity.avatarUrl} alt={activity.username} />
                    ) : (
                      activity.username.charAt(0).toUpperCase()
                    )}
                  </div>
                  <div className={styles.activityTextWrapper}>
                    <div className={styles.activityHeader}>
                      <span className={styles.activityUser}>{activity.username}</span>
                      <span className={styles.activityTime}>{new Date(activity.createdAt).toLocaleString()}</span>
                    </div>
                    <p className={styles.activityAction}>
                      {activity.description || activity.actionType.toLowerCase().replace('_', ' ')}
                    </p>
                  </div>
                </div>
              ))}
            </div>

            {activities.length > 5 && (
              <button 
                className={styles.showMoreBtn}
                onClick={() => setShowAllActivities(!showAllActivities)}
              >
                {showAllActivities ? 'Thu gọn' : `Xem thêm (${activities.length - 5} hoạt động khác)`}
              </button>
            )}
          </aside>
        </div>

        {/* hidden file input */}
        <input type="file" ref={fileInputRef} style={{ display: 'none' }} onChange={handleFileUpload} />

        {/* Popovers */}
        {showMemberPicker && (
          <div className={styles.popover} style={{ top: '150px', left: '24px' }}>
            <div className={styles.popoverHeader}>
              <span>Thành viên</span>
              <button onClick={() => { setShowMemberPicker(false); setMemberSearch(''); }}><MdClose /></button>
            </div>
            <div className={styles.popoverBody}>
              <div className={styles.popoverSearchWrapper}>
                <MdList className={styles.searchIcon} />
                <input 
                  className={styles.popoverSearchInput}
                  placeholder="Tìm kiếm thành viên..."
                  value={memberSearch}
                  onChange={(e) => setMemberSearch(e.target.value)}
                  autoFocus
                />
              </div>
              <div className={styles.popoverList}>
                {workspaceMembers
                  .filter(m => m.username.toLowerCase().includes(memberSearch.toLowerCase()))
                  .map(m => (
                    <div key={m.userId} className={styles.popoverMemberItem} onClick={() => handleAssignMember(m.userId)}>
                      <div className={styles.popoverAvatar}>
                        {m.avatarUrl ? <img src={m.avatarUrl} alt={m.username} /> : m.username.charAt(0).toUpperCase()}
                      </div>
                      <span className={styles.popoverName}>{m.username}</span>
                      {card.members.some(cm => cm.id === m.userId) && <span className={styles.checkMark}>✔</span>}
                    </div>
                  ))}
              </div>
            </div>
          </div>
        )}

        {showDatePicker && (
          <div className={styles.popover} style={{ top: '150px', left: '400px', width: '320px' }}>
            <div className={styles.popoverHeader}>
              <span>Thiết lập thời gian</span>
              <button onClick={() => setShowDatePicker(false)}><MdClose /></button>
            </div>
            <div className={styles.popoverBody}>
              <div className={styles.datePickerContent}>
                <div className={styles.dateFieldGroup}>
                  <label className={styles.metaLabel}>Ngày bắt đầu</label>
                  <div className={styles.dateInputWrapper}>
                    <FiCalendar className={styles.dateInputIcon} />
                    <input 
                      type="datetime-local" 
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                      className={styles.customDateInput}
                    />
                  </div>
                </div>

                <div className={styles.dateFieldGroup} style={{ marginTop: '16px' }}>
                  <label className={styles.metaLabel}>Ngày hết hạn</label>
                  <div className={styles.dateInputWrapper}>
                    <FiClock className={styles.dateInputIcon} />
                    <input 
                      type="datetime-local" 
                      value={dueDate}
                      onChange={(e) => setDueDate(e.target.value)}
                      className={styles.customDateInput}
                    />
                  </div>
                </div>

                <div className={styles.quickSelectGrid}>
                  <button onClick={() => {
                    const now = new Date();
                    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
                    setDueDate(now.toISOString().slice(0, 16));
                  }}>Hôm nay</button>
                  <button onClick={() => {
                    const tomorrow = new Date();
                    tomorrow.setDate(tomorrow.getDate() + 1);
                    tomorrow.setMinutes(tomorrow.getMinutes() - tomorrow.getTimezoneOffset());
                    setDueDate(tomorrow.toISOString().slice(0, 16));
                  }}>Ngày mai</button>
                  <button onClick={() => {
                    const nextWeek = new Date();
                    nextWeek.setDate(nextWeek.getDate() + 7);
                    nextWeek.setMinutes(nextWeek.getMinutes() - nextWeek.getTimezoneOffset());
                    setDueDate(nextWeek.toISOString().slice(0, 16));
                  }}>Tuần sau</button>
                </div>
              </div>

              <div className={styles.popoverActions} style={{ marginTop: '20px', display: 'flex', gap: '8px' }}>
                <button 
                  className={styles.saveBtn} 
                  style={{ flex: 1, padding: '10px' }}
                  onClick={() => {
                    handleUpdateCard({ startDate: startDate || null, dueDate: dueDate || null });
                    setShowDatePicker(false);
                  }}
                >
                  Lưu thiết lập
                </button>
                <button 
                  className={styles.removeDateBtn}
                  onClick={() => {
                    setStartDate('');
                    setDueDate('');
                    handleUpdateCard({ startDate: null, dueDate: null });
                  }}
                >
                  Gỡ bỏ
                </button>
              </div>
            </div>
          </div>
        )}

        {showLabelPicker && (
          <div className={styles.popover} style={{ top: '150px', left: '200px' }}>
            <div className={styles.popoverHeader}>
              <span>Nhãn</span>
              <button onClick={() => setShowLabelPicker(false)}>&times;</button>
            </div>
            <div className={styles.popoverBody}>
              <div className={styles.colorPresets}>
                {PRESET_COLORS.map(color => (
                  <div key={color} style={{ background: color }} className={styles.colorPreset} onClick={() => handleCreateAndAddLabel(color)} />
                ))}
              </div>
              <div className={styles.boardLabelsList}>
                {boardLabels.map(l => (
                  <div key={l.id} className={styles.boardLabelRow} onClick={() => handleAddLabel(l.id)} style={{ background: l.color }}>
                    {l.name}
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default CardDetailModal;

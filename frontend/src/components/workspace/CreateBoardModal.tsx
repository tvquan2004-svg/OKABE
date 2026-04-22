import { useState } from 'react';
import { useGetTemplatesQuery, type BoardTemplate } from '../../services/templateApi';
import styles from './CreateBoardModal.module.css';

interface CreateBoardModalProps {
  workspaceId: number;
  isOpen: boolean;
  isSubmitting: boolean;
  onClose: () => void;
  onSubmit: (data: { name: string; description?: string; templateId?: number }) => void;
}

function CreateBoardModal({
  workspaceId,
  isOpen,
  isSubmitting,
  onClose,
  onSubmit,
}: CreateBoardModalProps) {
  const [activeTab, setActiveTab] = useState<'blank' | 'template'>('blank');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [selectedTemplate, setSelectedTemplate] = useState<BoardTemplate | null>(null);

  const { data: templatesRes, isLoading: isLoadingTemplates } = useGetTemplatesQuery({ workspaceId });
  const templates = templatesRes?.data || [];

  if (!isOpen) return null;

  const handleSubmit = () => {
    if (!name.trim()) return;
    onSubmit({
      name: name.trim(),
      description: description.trim() || undefined,
      templateId: activeTab === 'template' ? selectedTemplate?.id : undefined,
    });
  };

  const selectTemplate = (template: BoardTemplate) => {
    setSelectedTemplate(template);
    if (!name.trim()) {
      setName(template.name);
    }
  };

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <div className={styles.header}>
          <h2>Create board</h2>
          <button className={styles.closeBtn} onClick={onClose}>&times;</button>
        </div>

        <div className={styles.tabs}>
          <button
            className={`${styles.tab} ${activeTab === 'blank' ? styles.activeTab : ''}`}
            onClick={() => setActiveTab('blank')}
          >
            Blank board
          </button>
          <button
            className={`${styles.tab} ${activeTab === 'template' ? styles.activeTab : ''}`}
            onClick={() => setActiveTab('template')}
          >
            Use a template
          </button>
        </div>

        <div className={styles.body}>
          <div className={styles.field}>
            <label htmlFor="board-name">Board name</label>
            <input
              id="board-name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Project Alpha"
              className={styles.input}
              autoFocus
            />
          </div>

          <div className={styles.field}>
            <label htmlFor="board-desc">Description (optional)</label>
            <textarea
              id="board-desc"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="What is this board about?"
              className={styles.textarea}
              rows={2}
            />
          </div>

          {activeTab === 'template' && (
            <div className={styles.templatesSection}>
              <h3>Select a template</h3>
              {isLoadingTemplates ? (
                <p className={styles.loading}>Loading templates...</p>
              ) : (
                <div className={styles.templateGrid}>
                  {templates.map((template) => (
                    <div
                      key={template.id}
                      className={`${styles.templateCard} ${
                        selectedTemplate?.id === template.id ? styles.selectedTemplate : ''
                      }`}
                      onClick={() => selectTemplate(template)}
                    >
                      <h4>{template.name}</h4>
                      <p>{template.description}</p>
                      <div className={styles.listPreview}>
                        {template.lists?.slice(0, 3).map((list) => (
                          <span key={list.id} className={styles.listChip}>
                            {list.name}
                          </span>
                        ))}
                        {(template.lists?.length || 0) > 3 && (
                          <span className={styles.moreLabel}>
                            +{template.lists!.length - 3} more
                          </span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        <div className={styles.footer}>
          <button className={`btn ${styles.cancelBtn}`} onClick={onClose}>
            Cancel
          </button>
          <button
            className="btn btn-primary"
            onClick={handleSubmit}
            disabled={isSubmitting || !name.trim() || (activeTab === 'template' && !selectedTemplate)}
          >
            {isSubmitting ? 'Creating...' : 'Create board'}
          </button>
        </div>
      </div>
    </div>
  );
}

export default CreateBoardModal;

import styles from './EntityModal.module.css';

interface EntityModalProps {
  title: string;
  nameLabel: string;
  nameValue: string;
  namePlaceholder: string;
  submitLabel: string;
  isSubmitting?: boolean;
  onNameChange: (value: string) => void;
  onClose: () => void;
  onSubmit: () => void;
  showDescription?: boolean;
  descriptionLabel?: string;
  descriptionValue?: string;
  descriptionPlaceholder?: string;
  onDescriptionChange?: (value: string) => void;
}

function EntityModal({
  title,
  nameLabel,
  nameValue,
  namePlaceholder,
  submitLabel,
  isSubmitting = false,
  onNameChange,
  onClose,
  onSubmit,
  showDescription = true,
  descriptionLabel = 'Mô tả',
  descriptionValue = '',
  descriptionPlaceholder = 'Mô tả không bắt buộc...',
  onDescriptionChange,
}: EntityModalProps) {
  const isSubmitDisabled = isSubmitting || !nameValue.trim();

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={(event) => event.stopPropagation()}>
        <h2 className={styles.title}>{title}</h2>

        <div className={styles.field}>
          <label htmlFor="entity-name">{nameLabel}</label>
          <input
            id="entity-name"
            value={nameValue}
            onChange={(event) => onNameChange(event.target.value)}
            placeholder={namePlaceholder}
            className={styles.input}
            autoFocus
          />
        </div>

        {showDescription && onDescriptionChange ? (
          <div className={styles.field}>
            <label htmlFor="entity-description">{descriptionLabel}</label>
            <textarea
              id="entity-description"
              value={descriptionValue}
              onChange={(event) => onDescriptionChange(event.target.value)}
              placeholder={descriptionPlaceholder}
              className={styles.textarea}
              rows={3}
            />
          </div>
        ) : null}

        <div className={styles.actions}>
          <button className="btn btn-outline" onClick={onClose}>
            Hủy
          </button>
          <button
            className="btn btn-primary"
            onClick={onSubmit}
            disabled={isSubmitDisabled}
          >
            {isSubmitting ? 'Đang lưu...' : submitLabel}
          </button>
        </div>
      </div>
    </div>
  );
}

export default EntityModal;

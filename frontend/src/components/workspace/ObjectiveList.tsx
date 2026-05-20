import React, { useState } from 'react';
import {
  useGetObjectivesQuery,
  useCreateObjectiveMutation,
  useAddKeyResultMutation,
  useDeleteObjectiveMutation,
  useRecalculateProgressMutation,
  type Objective,
} from '../../services/okrApi';
import KeyResultRow from './KeyResultRow';
import styles from './ObjectiveList.module.css';

interface ObjectiveListProps {
  workspaceId: number;
}

const QUARTERS = ['2025-Q1', '2025-Q2', '2025-Q3', '2025-Q4', '2026-Q1', '2026-Q2', '2026-Q3', '2026-Q4'];

const currentQuarter = () => {
  const now = new Date();
  const q = Math.ceil((now.getMonth() + 1) / 3);
  return `${now.getFullYear()}-Q${q}`;
};

const ObjectiveList: React.FC<ObjectiveListProps> = ({ workspaceId }) => {
  const [quarter, setQuarter] = useState(currentQuarter());
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [showKrForm, setShowKrForm] = useState<number | null>(null);

  const { data: objectivesRes } = useGetObjectivesQuery({ workspaceId, quarter });
  const [createObjective] = useCreateObjectiveMutation();
  const [addKeyResult] = useAddKeyResultMutation();
  const [deleteObjective] = useDeleteObjectiveMutation();
  const [recalculate] = useRecalculateProgressMutation();

  const objectives = objectivesRes?.data ?? [];

  return (
    <div className={styles.container}>
      <div className={styles.toolbar}>
        <div className={styles.quarterSelect}>
          <label>Quý:</label>
          <select value={quarter} onChange={(e) => setQuarter(e.target.value)}>
            {QUARTERS.map((q) => (
              <option key={q} value={q}>{q}</option>
            ))}
          </select>
        </div>
        <button className={styles.addBtn} onClick={() => setShowCreateForm(true)}>
          + Mục tiêu
        </button>
      </div>

      {showCreateForm && (
        <CreateObjectiveForm
          workspaceId={workspaceId}
          quarter={quarter}
          onSave={async (data) => {
            await createObjective({ workspaceId, ...data });
            setShowCreateForm(false);
          }}
          onCancel={() => setShowCreateForm(false)}
        />
      )}

      {objectives.map((obj) => (
        <ObjectiveCard
          key={obj.id}
          objective={obj}
          workspaceId={workspaceId}
          expanded={expandedId === obj.id}
          onToggle={() => setExpandedId(expandedId === obj.id ? null : obj.id)}
          showKrForm={showKrForm === obj.id}
          onAddKr={() => setShowKrForm(showKrForm === obj.id ? null : obj.id)}
          onKrSubmit={async (data) => {
            await addKeyResult({ objectiveId: obj.id, ...data });
            setShowKrForm(null);
          }}
          onDelete={async () => {
            await deleteObjective(obj.id);
          }}
          onRecalculate={async () => {
            await recalculate(obj.id);
          }}
        />
      ))}

      {objectives.length === 0 && (
        <div className={styles.empty}>
          <p>Chưa có mục tiêu nào cho {quarter}</p>
          <button className={styles.addBtn} onClick={() => setShowCreateForm(true)}>
            + Tạo mục tiêu đầu tiên
          </button>
        </div>
      )}
    </div>
  );
};

const CreateObjectiveForm: React.FC<{
  workspaceId: number;
  quarter: string;
  onSave: (data: { title: string; description?: string; quarter: string }) => void;
  onCancel: () => void;
}> = ({ quarter, onSave, onCancel }) => {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');

  return (
    <div className={styles.createForm}>
      <input
        className={styles.formInput}
        placeholder="Tiêu đề mục tiêu"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
      />
      <textarea
        className={styles.formTextarea}
        placeholder="Mô tả (không bắt buộc)"
        value={description}
        onChange={(e) => setDescription(e.target.value)}
        rows={2}
      />
      <div className={styles.formActions}>
        <button className={styles.cancelBtn} onClick={onCancel}>Hủy</button>
        <button
          className={styles.saveBtn}
          disabled={!title.trim()}
          onClick={() => onSave({ title: title.trim(), description: description.trim() || undefined, quarter })}
        >
          Tạo
        </button>
      </div>
    </div>
  );
};

const ObjectiveCard: React.FC<{
  objective: Objective;
  workspaceId: number;
  expanded: boolean;
  onToggle: () => void;
  showKrForm: boolean;
  onAddKr: () => void;
  onKrSubmit: (data: { title: string; targetValue?: number; unit?: string }) => void;
  onDelete: () => void;
  onRecalculate: () => void;
}> = ({ objective, workspaceId, expanded, onToggle, showKrForm, onAddKr, onKrSubmit, onDelete, onRecalculate }) => {
  const progress = Math.round(objective.progress);
  const progressColor = progress >= 70 ? 'var(--color-success)' : progress >= 30 ? 'var(--color-warning)' : 'var(--color-primary)';

  return (
    <div className={styles.objectiveCard}>
      <div className={styles.objHeader} onClick={onToggle}>
        <div className={styles.objInfo}>
          <span className={styles.expandIcon}>{expanded ? '▼' : '▶'}</span>
          <div className={styles.objTitleWrap}>
            <h3 className={styles.objTitle}>{objective.title}</h3>
            {objective.description && (
              <p className={styles.objDesc}>{objective.description}</p>
            )}
          </div>
        </div>
        <div className={styles.objMeta}>
          <div className={styles.objProgress}>
            <div className={styles.objProgressTrack}>
              <div
                className={styles.objProgressFill}
                style={{ width: `${progress}%`, background: progressColor }}
              />
            </div>
            <span className={styles.objProgressLabel}>{progress}%</span>
          </div>
          <div className={styles.objActions}>
            <button className={styles.actionBtn} onClick={(e) => { e.stopPropagation(); onRecalculate(); }} title="Tính lại">
              ⟳
            </button>
            <button className={styles.deleteBtn} onClick={(e) => { e.stopPropagation(); onDelete(); }} title="Xóa">
              🗑
            </button>
          </div>
        </div>
      </div>

      {expanded && (
        <div className={styles.objBody}>
          {objective.keyResults.length > 0 && (
            <div className={styles.krList}>
              {objective.keyResults.map((kr) => (
                <KeyResultRow key={kr.id} kr={kr} objectiveId={objective.id} workspaceId={workspaceId} />
              ))}
            </div>
          )}

          {showKrForm && (
            <KrForm onSubmit={onKrSubmit} onCancel={onAddKr} />
          )}

          <button className={styles.addKrBtn} onClick={onAddKr}>
            + Key Result
          </button>
        </div>
      )}
    </div>
  );
};

const KrForm: React.FC<{
  onSubmit: (data: { title: string; targetValue?: number; unit?: string }) => void;
  onCancel: () => void;
}> = ({ onSubmit, onCancel }) => {
  const [title, setTitle] = useState('');
  const [targetValue, setTargetValue] = useState('');
  const [unit, setUnit] = useState('percent');

  return (
    <div className={styles.krForm}>
      <input
        className={styles.formInput}
        placeholder="Tên key result"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
      />
      <div className={styles.krFormRow}>
        <input
          className={styles.formInput}
          placeholder="Giá trị mục tiêu"
          value={targetValue}
          onChange={(e) => setTargetValue(e.target.value)}
          type="number"
        />
        <select className={styles.formSelect} value={unit} onChange={(e) => setUnit(e.target.value)}>
          <option value="percent">%</option>
          <option value="count">số lượng</option>
          <option value="currency">VNĐ</option>
        </select>
      </div>
      <div className={styles.formActions}>
        <button className={styles.cancelBtn} onClick={onCancel}>Hủy</button>
        <button className={styles.saveBtn} onClick={() => onSubmit({ title, targetValue: targetValue ? Number(targetValue) : undefined, unit })}>
          Thêm
        </button>
      </div>
    </div>
  );
};

export default ObjectiveList;

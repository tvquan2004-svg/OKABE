import React, { useState, useEffect } from 'react';
import { FiSearch, FiFilter } from 'react-icons/fi';
import { type Label, type User, type CardSearchParams } from '../../services/boardApi';
import { useDebounce } from '../../hooks/useDebounce';
import styles from './BoardFilter.module.css';

interface BoardFilterProps {
  labels: Label[];
  members: User[];
  onFilterChange: (filters: CardSearchParams) => void;
}

const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

export const BoardFilter: React.FC<BoardFilterProps> = ({ labels, members, onFilterChange }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [selectedLabels, setSelectedLabels] = useState<number[]>([]);
  const [selectedMembers, setSelectedMembers] = useState<number[]>([]);
  const [selectedPriorities, setSelectedPriorities] = useState<string[]>([]);
  const [dueDateFrom, setDueDateFrom] = useState('');
  const [dueDateTo, setDueDateTo] = useState('');
  const [isOverdue, setIsOverdue] = useState(false);

  const debouncedKeyword = useDebounce(keyword, 300);

  useEffect(() => {
    onFilterChange({
      keyword: debouncedKeyword,
      labelIds: selectedLabels.length > 0 ? selectedLabels : undefined,
      assigneeIds: selectedMembers.length > 0 ? selectedMembers : undefined,
      priorities: selectedPriorities.length > 0 ? selectedPriorities : undefined,
      dueDateFrom: dueDateFrom || undefined,
      dueDateTo: dueDateTo || undefined,
      isOverdue: isOverdue || undefined,
    });
  }, [debouncedKeyword, selectedLabels, selectedMembers, selectedPriorities, dueDateFrom, dueDateTo, isOverdue]);

  const toggleLabel = (id: number) => {
    setSelectedLabels(prev => prev.includes(id) ? prev.filter(i => i !== id) : [...prev, id]);
  };

  const toggleMember = (id: number) => {
    setSelectedMembers(prev => prev.includes(id) ? prev.filter(i => i !== id) : [...prev, id]);
  };

  const togglePriority = (p: string) => {
    setSelectedPriorities(prev => prev.includes(p) ? prev.filter(i => i !== p) : [...prev, p]);
  };

  const clearAll = () => {
    setKeyword('');
    setSelectedLabels([]);
    setSelectedMembers([]);
    setSelectedPriorities([]);
    setDueDateFrom('');
    setDueDateTo('');
    setIsOverdue(false);
  };

  const activeCount = 
    (keyword ? 1 : 0) + 
    selectedLabels.length + 
    selectedMembers.length + 
    selectedPriorities.length + 
    (dueDateFrom || dueDateTo ? 1 : 0) +
    (isOverdue ? 1 : 0);

  return (
    <div className={styles.filterContainer}>
      <div className={styles.searchBar}>
        <div className={styles.searchInputWrapper}>
          <FiSearch className={styles.searchIcon} />
          <input
            type="text"
            className={styles.searchInput}
            placeholder="Search cards by title or description..."
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
          />
        </div>
        <button 
          className={`${styles.filterToggleBtn} ${isOpen ? styles.active : ''}`}
          onClick={() => setIsOpen(!isOpen)}
        >
          <FiFilter />
          Filters {activeCount > 0 && `(${activeCount})`}
        </button>
        {activeCount > 0 && (
          <button className={styles.clearAllBtn} onClick={clearAll}>Clear all</button>
        )}
      </div>

      {isOpen && (
        <div className={styles.filterPanel}>
          <div className={styles.filterGroup}>
            <span className={styles.filterLabel}>Labels</span>
            <div className={styles.multiSelect}>
              {labels.map(label => (
                <div 
                  key={label.id}
                  className={`${styles.filterChip} ${selectedLabels.includes(label.id) ? styles.selected : ''}`}
                  style={selectedLabels.includes(label.id) ? { backgroundColor: label.color, borderColor: label.color } : {}}
                  onClick={() => toggleLabel(label.id)}
                >
                  {label.name}
                </div>
              ))}
            </div>
          </div>

          <div className={styles.filterGroup}>
            <span className={styles.filterLabel}>Assignees</span>
            <div className={styles.multiSelect}>
              {members.map(member => (
                <div 
                  key={member.id}
                  className={`${styles.filterChip} ${selectedMembers.includes(member.id) ? styles.selected : ''}`}
                  onClick={() => toggleMember(member.id)}
                >
                  {member.username}
                </div>
              ))}
            </div>
          </div>

          <div className={styles.filterGroup}>
            <span className={styles.filterLabel}>Priority</span>
            <div className={styles.multiSelect}>
              {PRIORITIES.map(p => (
                <div 
                  key={p}
                  className={`${styles.filterChip} ${selectedPriorities.includes(p) ? styles.selected : ''}`}
                  onClick={() => togglePriority(p)}
                >
                  {p}
                </div>
              ))}
            </div>
          </div>

          <div className={styles.filterGroup}>
            <span className={styles.filterLabel}>Due Date</span>
            <div className={styles.dateRange}>
              <input 
                type="date" 
                className={styles.dateInput}
                value={dueDateFrom}
                onChange={(e) => setDueDateFrom(e.target.value)}
              />
              <span style={{ color: '#94a3b8' }}>to</span>
              <input 
                type="date" 
                className={styles.dateInput}
                value={dueDateTo}
                onChange={(e) => setDueDateTo(e.target.value)}
              />
            </div>
            <label style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#e2e8f0', fontSize: '0.85rem', cursor: 'pointer', marginTop: '4px' }}>
              <input 
                type="checkbox" 
                checked={isOverdue}
                onChange={(e) => setIsOverdue(e.target.checked)}
              />
              Overdue cards only
            </label>
          </div>
        </div>
      )}
    </div>
  );
};

export default BoardFilter;

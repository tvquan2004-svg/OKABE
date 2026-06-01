import { useState, useMemo, useCallback } from 'react';
import {
  useGetWorkloadQuery,
  type MemberWorkload,
  type DayWorkload,
} from '../../services/workspaceApi';
import { FiRefreshCw } from 'react-icons/fi';
import styles from './WorkloadHeatmap.module.css';

interface WorkloadHeatmapProps {
  workspaceId: number;
}

interface TooltipState {
  userId: number;
  date: string;
  x: number;
  y: number;
}

const getCellClass = (day: DayWorkload | undefined) => {
  if (!day || day.cardCount === 0) return styles.cellEmpty;
  if (day.overloaded) return styles.cellOverloaded;
  const h = day.totalHours;
  if (h <= 2) return styles.cellLow;
  if (h <= 4) return styles.cellMedium;
  if (h <= 6) return styles.cellHigh;
  return styles.cellVeryHigh;
};

const formatDate = (dateStr: string) => {
  const d = new Date(dateStr + 'T00:00:00');
  return d.toLocaleDateString('vi-VN', { weekday: 'short', day: 'numeric', month: 'numeric' });
};

function WorkloadHeatmap({ workspaceId }: WorkloadHeatmapProps) {
  const today = new Date();
  const defaultFrom = new Date(today.getFullYear(), today.getMonth(), 1);
  const defaultTo = new Date(today.getFullYear(), today.getMonth() + 1, 0);

  const [from, setFrom] = useState(defaultFrom.toISOString().slice(0, 10));
  const [to, setTo] = useState(defaultTo.toISOString().slice(0, 10));
  const [tooltip, setTooltip] = useState<TooltipState | null>(null);

  const { data: workloadRes, isLoading } = useGetWorkloadQuery({ workspaceId, from, to });

  const workload = workloadRes?.data;

  const days = useMemo(() => {
    const d = [];
    const start = new Date(from + 'T00:00:00');
    const end = new Date(to + 'T00:00:00');
    for (let dt = new Date(start); dt <= end; dt.setDate(dt.getDate() + 1)) {
      d.push(dt.toISOString().slice(0, 10));
    }
    return d;
  }, [from, to]);

  const getDayData = useCallback(
    (member: MemberWorkload, date: string): DayWorkload | undefined => {
      return member.workload.find((w) => w.date === date);
    },
    []
  );

  const handleReassign = () => {
    alert('🧠 Gợi ý reassign: Tính năng đang phát triển. Sẽ gợi ý phân bổ lại công việc dựa trên workload hiện tại.');
  };

  return (
    <div className={styles.container}>
      <div className={styles.toolbar}>
        <div className={styles.dateRange}>
          <label>Từ</label>
          <input
            type="date"
            className={styles.dateInput}
            value={from}
            onChange={(e) => setFrom(e.target.value)}
          />
          <label>Đến</label>
          <input
            type="date"
            className={styles.dateInput}
            value={to}
            onChange={(e) => setTo(e.target.value)}
          />
        </div>
        <button className={styles.reassignBtn} onClick={handleReassign}>
          <FiRefreshCw size={14} />
          Gợi ý reassign
        </button>
      </div>

      {isLoading ? (
        <div className={styles.loading}>Đang tải dữ liệu workload...</div>
      ) : !workload || workload.members.length === 0 ? (
        <div className={styles.emptyState}>
          Chưa có dữ liệu workload trong khoảng thời gian này.
        </div>
      ) : (
        <>
          <div className={styles.heatmapWrapper}>
            <table className={styles.heatmapTable}>
              <thead>
                <tr className={styles.headerRow}>
                  <th style={{ width: '150px', textAlign: 'left' }}>Thành viên</th>
                  {days.map((date) => {
                    const d = new Date(date + 'T00:00:00');
                    const dayNum = d.getDate();
                    const isFirstOfMonth = dayNum === 1;
                    return (
                      <th
                        key={date}
                        style={{
                          borderLeft: d.getDay() === 1 ? '1px solid rgba(255,255,255,0.06)' : undefined,
                          color: isFirstOfMonth ? 'var(--color-text-secondary)' : undefined,
                        }}
                      >
                        {isFirstOfMonth
                          ? d.toLocaleDateString('vi-VN', { month: 'short' }) + '\n' + dayNum
                          : dayNum}
                      </th>
                    );
                  })}
                </tr>
              </thead>
              <tbody>
                {workload.members.map((member) => (
                  <tr key={member.userId}>
                    <td>
                      <div className={styles.memberLabel}>
                        {member.avatarUrl ? (
                          <img src={member.avatarUrl} alt={member.userName} />
                        ) : (
                          <div className={styles.memberAvatar}>
                            {member.userName.charAt(0).toUpperCase()}
                          </div>
                        )}
                        {member.userName}
                      </div>
                    </td>
                    {days.map((date) => {
                      const dayData = getDayData(member, date);
                      return (
                        <td key={date} style={{ position: 'relative' }}>
                          <div
                            className={`${styles.cell} ${getCellClass(dayData)}`}
                            onMouseMove={(e) => setTooltip({ userId: member.userId, date, x: e.clientX + 12, y: e.clientY - 10 })}
                            onMouseLeave={() => setTooltip(null)}
                          />
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {tooltip && (() => {
            const dayData = getDayData(
              workload.members.find((m) => m.userId === tooltip.userId)!,
              tooltip.date
            );
            if (!dayData) return null;
            return (
              <div
                className={styles.tooltip}
                style={{ left: tooltip.x, top: tooltip.y }}
              >
                <div className={styles.tooltipTitle}>
                  {formatDate(tooltip.date)}
                </div>
                <div className={styles.tooltipHours}>
                  {dayData.cardCount} thẻ · {dayData.totalHours}h
                  {dayData.overloaded ? ' ⚠️ Quá tải' : ''}
                </div>
              </div>
            );
          })()}

          <div className={styles.legend}>
            <span className={styles.legendLabel}>Ít</span>
            <div className={styles.legendItem}>
              <div className={`${styles.legendSwatch} ${styles.cellEmpty}`} /> 0
            </div>
            <div className={styles.legendItem}>
              <div className={`${styles.legendSwatch} ${styles.cellLow}`} /> 1-2h
            </div>
            <div className={styles.legendItem}>
              <div className={`${styles.legendSwatch} ${styles.cellMedium}`} /> 3-4h
            </div>
            <div className={styles.legendItem}>
              <div className={`${styles.legendSwatch} ${styles.cellHigh}`} /> 5-6h
            </div>
            <div className={styles.legendItem}>
              <div className={`${styles.legendSwatch} ${styles.cellVeryHigh}`} /> 7-8h
            </div>
            <div className={styles.legendItem}>
              <div className={`${styles.legendSwatch} ${styles.cellOverloaded}`} /> &gt;8h
            </div>
            <span className={styles.legendLabel}>Nhiều</span>
          </div>
        </>
      )}
    </div>
  );
}

export default WorkloadHeatmap;

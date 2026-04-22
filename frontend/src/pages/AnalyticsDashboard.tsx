import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
  PieChart, Pie, Cell, AreaChart, Area
} from 'recharts';
import { 
  FiArrowLeft, FiBarChart2, FiUsers, FiCheckCircle, FiAlertCircle, 
  FiClock, FiCalendar, FiDownload, FiActivity
} from 'react-icons/fi';
import { useGetBoardAnalyticsQuery, useGetBoardQuery } from '../services/boardApi';
import styles from './AnalyticsDashboard.module.css';

const PRIORITY_COLORS: Record<string, string> = {
  CRITICAL: '#ef4444',
  HIGH: '#f59e0b',
  MEDIUM: '#3b82f6',
  LOW: '#94a3b8',
};

const AnalyticsDashboard: React.FC = () => {
  const { boardId } = useParams<{ boardId: string }>();
  const navigate = useNavigate();
  const id = Number(boardId);

  const { data: boardRes } = useGetBoardQuery(id);
  const { data: analyticsRes, isLoading, error } = useGetBoardAnalyticsQuery(id);

  if (isLoading) return <div className={styles.loading}>Đang tải dữ liệu phân tích...</div>;
  if (error || !analyticsRes) return <div className={styles.error}>Lỗi khi tải dữ liệu phân tích</div>;

  const board = boardRes?.data;
  const stats = analyticsRes.data;

  const totalCards = stats.cardsByStatus.reduce((acc, curr) => acc + curr.total, 0);
  const totalOverdue = stats.cardsByStatus.reduce((acc, curr) => acc + curr.overdue, 0);
  const totalCompletedThisWeek = stats.cardsByStatus.reduce((acc, curr) => acc + curr.completedThisWeek, 0);

  const exportToCSV = () => {
    // Basic CSV export logic
    const headers = ['Mục', 'Tổng cộng', 'Quá hạn', 'Hoàn thành tuần này'];
    const rows = stats.cardsByStatus.map(s => [s.listName, s.total, s.overdue, s.completedThisWeek]);
    
    const csvContent = "data:text/csv;charset=utf-8," 
      + headers.join(",") + "\n"
      + rows.map(e => e.join(",")).join("\n");

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `analytics_${board?.name || 'board'}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.titleSection}>
          <button className="btn btn-outline" onClick={() => navigate(`/board/${id}`)}>
            <FiArrowLeft /> Quay lại
          </button>
          <div className={styles.headerInfo}>
            <h1>Phân tích & Báo cáo</h1>
            <p>{board?.name} • Dữ liệu cập nhật thời gian thực</p>
          </div>
        </div>
        <button className="btn btn-primary" onClick={exportToCSV}>
          <FiDownload /> Xuất CSV
        </button>
      </header>

      {/* Summary Cards */}
      <div className={styles.summaryGrid}>
        <div className={styles.statCard}>
          <div className={`${styles.statIcon} ${styles.blue}`}>
            <FiBarChart2 />
          </div>
          <div className={styles.statContent}>
            <span className={styles.statLabel}>Tổng số thẻ</span>
            <span className={styles.statValue}>{totalCards}</span>
          </div>
        </div>
        <div className={styles.statCard}>
          <div className={`${styles.statIcon} ${styles.red}`}>
            <FiAlertCircle />
          </div>
          <div className={styles.statContent}>
            <span className={styles.statLabel}>Thẻ quá hạn</span>
            <span className={styles.statValue}>{totalOverdue}</span>
          </div>
        </div>
        <div className={styles.statCard}>
          <div className={`${styles.statIcon} ${styles.green}`}>
            <FiCheckCircle />
          </div>
          <div className={styles.statContent}>
            <span className={styles.statLabel}>Xong tuần này</span>
            <span className={styles.statValue}>{totalCompletedThisWeek}</span>
          </div>
        </div>
        <div className={styles.statCard}>
          <div className={`${styles.statIcon} ${styles.purple}`}>
            <FiClock />
          </div>
          <div className={styles.statContent}>
            <span className={styles.statLabel}>T/g hoàn thành TB</span>
            <span className={styles.statValue}>{stats.avgCompletionDays.toFixed(1)} ngày</span>
          </div>
        </div>
      </div>

      <div className={styles.mainGrid}>
        {/* Burndown Chart */}
        <div className={styles.chartCard}>
          <div className={styles.chartHeader}>
            <h3>Biểu đồ Burndown (30 ngày)</h3>
            <FiActivity />
          </div>
          <div className={styles.chartContainer}>
            <ResponsiveContainer width="100%" height={300}>
              <AreaChart data={stats.burndown}>
                <defs>
                  <linearGradient id="colorRemaining" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.1}/>
                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--color-border)" />
                <XAxis 
                  dataKey="date" 
                  tick={{fill: 'var(--color-text-secondary)', fontSize: 12}}
                  axisLine={false}
                  tickLine={false}
                />
                <YAxis 
                  tick={{fill: 'var(--color-text-secondary)', fontSize: 12}}
                  axisLine={false}
                  tickLine={false}
                />
                <Tooltip 
                  contentStyle={{ backgroundColor: 'var(--color-bg-secondary)', borderColor: 'var(--color-border)', borderRadius: '8px' }}
                />
                <Legend />
                <Area 
                  type="monotone" 
                  dataKey="remaining" 
                  name="Còn lại"
                  stroke="#3b82f6" 
                  fillOpacity={1} 
                  fill="url(#colorRemaining)" 
                />
                <Area 
                  type="monotone" 
                  dataKey="completed" 
                  name="Hoàn thành"
                  stroke="#22c55e" 
                  fillOpacity={0} 
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Priority Donut Chart */}
        <div className={styles.chartCard}>
          <div className={styles.chartHeader}>
            <h3>Phân bổ mức độ ưu tiên</h3>
            <FiAlertCircle />
          </div>
          <div className={styles.chartContainer}>
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={stats.cardsByPriority}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={80}
                  paddingAngle={5}
                  dataKey="count"
                  nameKey="priority"
                >
                  {stats.cardsByPriority.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={PRIORITY_COLORS[entry.priority] || '#64748b'} />
                  ))}
                </Pie>
                <Tooltip 
                  contentStyle={{ backgroundColor: 'var(--color-bg-secondary)', borderColor: 'var(--color-border)', borderRadius: '8px' }}
                />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Status Bar Chart */}
        <div className={styles.chartCard}>
          <div className={styles.chartHeader}>
            <h3>Trạng thái thẻ theo danh sách</h3>
            <FiBarChart2 />
          </div>
          <div className={styles.chartContainer}>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={stats.cardsByStatus}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--color-border)" />
                <XAxis 
                  dataKey="listName" 
                  tick={{fill: 'var(--color-text-secondary)', fontSize: 12}}
                  axisLine={false}
                  tickLine={false}
                />
                <YAxis 
                  tick={{fill: 'var(--color-text-secondary)', fontSize: 12}}
                  axisLine={false}
                  tickLine={false}
                />
                <Tooltip 
                  contentStyle={{ backgroundColor: 'var(--color-bg-secondary)', borderColor: 'var(--color-border)', borderRadius: '8px' }}
                />
                <Bar dataKey="total" name="Tổng số" fill="#3b82f6" radius={[4, 4, 0, 0]} />
                <Bar dataKey="overdue" name="Quá hạn" fill="#ef4444" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Activity Heatmap */}
        <div className={styles.chartCard}>
          <div className={styles.chartHeader}>
            <h3>Hoạt động (90 ngày qua)</h3>
            <FiCalendar />
          </div>
          <div className={styles.chartContainer}>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={stats.activityHeatmap}>
                <XAxis dataKey="date" hide />
                <YAxis hide />
                <Tooltip 
                  contentStyle={{ backgroundColor: 'var(--color-bg-secondary)', borderColor: 'var(--color-border)', borderRadius: '8px' }}
                />
                <Bar dataKey="count" name="Hoạt động" fill="#8b5cf6" radius={[2, 2, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
            <p className={styles.heatmapHint}>Mỗi cột đại diện cho một ngày. Càng cao nghĩa là càng nhiều hoạt động.</p>
          </div>
        </div>
      </div>

      {/* Member Workload Table */}
      <section className={styles.workloadSection}>
        <div className={styles.sectionHeader}>
          <h3>Khối lượng công việc thành viên</h3>
          <FiUsers />
        </div>
        <div className={styles.tableContainer}>
          <table className={styles.workloadTable}>
            <thead>
              <tr>
                <th>Thành viên</th>
                <th>Thẻ đã giao</th>
                <th>Quá hạn</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {stats.cardsByMember.map(member => (
                <tr key={member.userId}>
                  <td>
                    <div className={styles.memberCell}>
                      {member.avatarUrl ? (
                        <img src={member.avatarUrl} alt={member.username} className={styles.avatar} />
                      ) : (
                        <div className={styles.avatarFallback}>{member.username.charAt(0).toUpperCase()}</div>
                      )}
                      <span>{member.username}</span>
                    </div>
                  </td>
                  <td>{member.assignedCount}</td>
                  <td className={member.overdueCount > 0 ? styles.overdueText : ''}>
                    {member.overdueCount}
                  </td>
                  <td>
                    <div className={styles.progressBar}>
                      <div 
                        className={styles.progressFill} 
                        style={{ 
                          width: `${member.assignedCount > 0 ? ((member.assignedCount - member.overdueCount) / member.assignedCount) * 100 : 100}%`,
                          backgroundColor: member.overdueCount > 0 ? 'var(--color-warning)' : 'var(--color-success)'
                        }} 
                      />
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
};

export default AnalyticsDashboard;

import { useState } from 'react';
import {
  useAddWorkspaceMemberMutation,
  useGetWorkspaceMembersQuery,
  useRemoveWorkspaceMemberMutation,
  useUpdateWorkspaceMemberRoleMutation,
  type WorkspaceMember,
} from '../../services/workspaceApi';
import styles from './MemberModal.module.css';

interface MemberModalProps {
  workspaceId: number;
  onClose: () => void;
  currentUserRole: string;
}

const MemberModal = ({ workspaceId, onClose, currentUserRole }: MemberModalProps) => {
  const { data: membersData, isLoading: isLoadingMembers } = useGetWorkspaceMembersQuery(workspaceId);
  const [addMember, { isLoading: isInviting }] = useAddWorkspaceMemberMutation();
  const [updateRole] = useUpdateWorkspaceMemberRoleMutation();
  const [removeMember] = useRemoveWorkspaceMemberMutation();

  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState('MEMBER');
  const [error, setError] = useState<string | null>(null);

  const members = membersData?.data ?? [];
  const canManage = currentUserRole === 'OWNER' || currentUserRole === 'ADMIN';

  const handleInvite = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!inviteEmail.trim()) return;

    setError(null);
    try {
      await addMember({
        workspaceId,
        body: { email: inviteEmail.trim(), role: inviteRole },
      }).unwrap();
      setInviteEmail('');
    } catch (err: unknown) {
      const e = err as { data?: { message?: string } };
      setError(e.data?.message || 'Không thể gửi lời mời. Vui lòng kiểm tra lại email hoặc thử lại sau.');
    }
  };

  const handleRoleChange = async (memberId: number, newRole: string) => {
    try {
      await updateRole({
        workspaceId,
        memberId,
        body: { role: newRole },
      }).unwrap();
    } catch (err: unknown) {
      const e = err as { data?: { message?: string } };
      alert(e.data?.message || 'Không thể cập nhật vai trò.');
    }
  };

  const handleRemove = async (memberId: number) => {
    if (!confirm('Bạn có chắc muốn xóa thành viên này khỏi workspace?')) return;
    try {
      await removeMember({ workspaceId, memberId }).unwrap();
    } catch (err: unknown) {
      const e = err as { data?: { message?: string } };
      alert(e.data?.message || 'Không thể xóa thành viên.');
    }
  };

  const getInitials = (name: string) => {
    return name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);
  };

  return (
    <div className={styles.modalOverlay} onClick={onClose}>
      <div className={styles.modalContent} onClick={(e) => e.stopPropagation()}>
        <div className={styles.modalHeader}>
          <h2>Thành viên workspace</h2>
          <button className={styles.closeBtn} onClick={onClose}>&times;</button>
        </div>

        <div className={styles.modalBody}>
          {canManage && (
            <div className={styles.inviteSection}>
              <h3>Mời thành viên</h3>
              <form className={styles.inviteForm} onSubmit={handleInvite}>
                <div className={styles.inputGroup}>
                  <input
                    type="email"
                    className={styles.inviteInput}
                    placeholder="Nhập địa chỉ email"
                    value={inviteEmail}
                    onChange={(e) => setInviteEmail(e.target.value)}
                    required
                  />
                  {error && <p className={styles.errorMsg}>{error}</p>}
                </div>
                <select
                  className={styles.roleSelect}
                  value={inviteRole}
                  onChange={(e) => setInviteRole(e.target.value)}
                >
                  <option value="ADMIN">Quản trị viên</option>
                  <option value="MEMBER">Thành viên</option>
                </select>
                <button type="submit" className="btn btn-primary" disabled={isInviting}>
                  {isInviting ? 'Đang mời...' : 'Gửi lời mời'}
                </button>
              </form>
            </div>
          )}

          <div className={styles.memberListSection}>
            <h3>
              Thành viên hiện tại <span className={styles.memberCount}>{members.length}</span>
            </h3>
            
            {isLoadingMembers ? (
              <div className={styles.loadingOverlay}>Đang tải thành viên...</div>
            ) : (
              <div className={styles.memberList}>
                {members.map((member: WorkspaceMember) => (
                  <div key={member.userId} className={styles.memberItem}>
                    <div className={styles.memberInfo}>
                      <div className={styles.avatar}>
                        {member.avatarUrl ? (
                          <img src={member.avatarUrl} alt={member.username} className={styles.avatarImg} />
                        ) : (
                          getInitials(member.username)
                        )}
                      </div>
                      <div className={styles.userDetails}>
                        <h4>{member.username}</h4>
                        <p>{member.email}</p>
                      </div>
                    </div>

                    <div className={styles.memberActions}>
                      {canManage && member.role !== 'OWNER' ? (
                        <>
                          <select
                            className={`${styles.roleSelect} ${styles.memberRoleSelect}`}
                            value={member.role}
                            onChange={(e) => handleRoleChange(member.userId, e.target.value)}
                          >
                            <option value="ADMIN">Quản trị viên</option>
                            <option value="MEMBER">Thành viên</option>
                          </select>
                          <button
                            className={`${styles.actionBtn} ${styles.removeBtn}`}
                            onClick={() => handleRemove(member.userId)}
                            title="Xóa thành viên"
                          >
                            Xóa
                          </button>
                        </>
                      ) : (
                        <span className={`${styles.roleBadge} ${styles[`role_${member.role}`]}`}>
                          {member.role}
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default MemberModal;

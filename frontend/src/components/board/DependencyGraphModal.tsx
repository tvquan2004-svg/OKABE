import React, { useMemo } from 'react';
import ReactFlow, {
  Node,
  Edge,
  Background,
  Controls,
  MarkerType,
  useNodesState,
  useEdgesState,
} from 'reactflow';
import 'reactflow/dist/style.css';
import { DependencyGraph } from '../../services/boardApi';

interface DependencyGraphModalProps {
  graph: DependencyGraph;
  onClose: () => void;
  onCardClick: (cardId: number) => void;
}

const getNodeColor = (listName: string, dueDate: string | null | undefined, isArchived: boolean) => {
  if (isArchived) return '#64748b';
  const lower = (listName || '').toLowerCase();
  const doneKeywords = ['done', 'completed', 'closed', 'hoàn thành'];
  const progressKeywords = ['in progress', 'doing', 'in review', 'đang thực hiện'];
  if (doneKeywords.some(kw => lower.includes(kw))) return '#22c55e';
  if (dueDate && new Date(dueDate) < new Date()) return '#ef4444';
  if (progressKeywords.some(kw => lower.includes(kw))) return '#f59e0b';
  return '#64748b';
};

const DependencyGraphModal: React.FC<DependencyGraphModalProps> = ({ graph, onClose, onCardClick }) => {
  const { card, blockedBy, blocking } = graph;

  const initialNodes: Node[] = useMemo(() => {
    const nodes: Node[] = [];
    const centerY = 250;

    blockedBy.forEach((c, i) => {
      nodes.push({
        id: `parent-${c.id}`,
        type: 'default',
        position: { x: 50, y: centerY - ((blockedBy.length - 1) * 80) / 2 + i * 160 },
        data: {
          label: c.title,
        },
        style: {
          background: getNodeColor(c.listName, c.dueDate, c.isArchived),
          color: '#fff',
          border: 'none',
          borderRadius: '12px',
          padding: '10px 16px',
          fontSize: '13px',
          fontWeight: 500,
          width: 220,
          cursor: 'pointer',
        },
      });
    });

    nodes.push({
      id: `current-${card.id}`,
      type: 'default',
      position: { x: 420, y: centerY - 30 },
      data: {
        label: `📌 ${card.title}`,
      },
      style: {
        background: getNodeColor(card.listName, card.dueDate, card.isArchived),
        color: '#fff',
        border: '2px solid rgba(255,255,255,0.3)',
        borderRadius: '14px',
        padding: '14px 20px',
        fontSize: '14px',
        fontWeight: 600,
        width: 260,
        cursor: 'pointer',
        boxShadow: '0 4px 20px rgba(0,0,0,0.3)',
      },
    });

    blocking.forEach((c, i) => {
      nodes.push({
        id: `child-${c.id}`,
        type: 'default',
        position: { x: 820, y: centerY - ((blocking.length - 1) * 80) / 2 + i * 160 },
        data: {
          label: c.title,
        },
        style: {
          background: getNodeColor(c.listName, c.dueDate, c.isArchived),
          color: '#fff',
          border: 'none',
          borderRadius: '12px',
          padding: '10px 16px',
          fontSize: '13px',
          fontWeight: 500,
          width: 220,
          cursor: 'pointer',
        },
      });
    });

    return nodes;
  }, [card, blockedBy, blocking]);

  const edges: Edge[] = useMemo(() => {
    const result: Edge[] = [];
    blockedBy.forEach((c) => {
      result.push({
        id: `e-parent-${c.id}-current`,
        source: `parent-${c.id}`,
        target: `current-${card.id}`,
        type: 'smoothstep',
        animated: true,
        markerEnd: { type: MarkerType.ArrowClosed, color: '#94a3b8' },
        style: { stroke: '#94a3b8', strokeWidth: 2 },
      });
    });
    blocking.forEach((c) => {
      result.push({
        id: `e-current-child-${c.id}`,
        source: `current-${card.id}`,
        target: `child-${c.id}`,
        type: 'smoothstep',
        animated: true,
        markerEnd: { type: MarkerType.ArrowClosed, color: '#94a3b8' },
        style: { stroke: '#94a3b8', strokeWidth: 2 },
      });
    });
    return result;
  }, [card, blockedBy, blocking]);

  const [nodesState, , onNodesChange] = useNodesState(initialNodes);
  const [edgesState, , onEdgesChange] = useEdgesState(edges);

  const onNodeClick = (_: React.MouseEvent, node: Node) => {
    const id = node.id.replace('parent-', '').replace('child-', '').replace('current-', '');
    onCardClick(Number(id));
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0,0,0,0.6)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
      }}
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <div
        style={{
          width: '90vw',
          height: '85vh',
          background: '#1e293b',
          borderRadius: '16px',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
          boxShadow: '0 20px 60px rgba(0,0,0,0.5)',
        }}
      >
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '16px 24px',
            borderBottom: '1px solid #334155',
          }}
        >
          <h3 style={{ margin: 0, color: '#f1f5f9', fontSize: '18px', fontWeight: 600 }}>
            🔗 Sơ đồ phụ thuộc
          </h3>
          <button
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              color: '#94a3b8',
              fontSize: '24px',
              cursor: 'pointer',
              padding: '4px 8px',
              borderRadius: '8px',
            }}
          >
            ✕
          </button>
        </div>

        <div style={{ flex: 1, overflow: 'hidden' }}>
          <ReactFlow
            nodes={nodesState}
            edges={edgesState}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onNodeClick={onNodeClick}
            fitView
            attributionPosition="bottom-left"
          >
            <Background color="#334155" gap={20} />
            <Controls
              style={{
                background: '#0f172a',
                border: '1px solid #334155',
                borderRadius: '8px',
              }}
            />
          </ReactFlow>
        </div>

        <div
          style={{
            padding: '10px 24px',
            borderTop: '1px solid #334155',
            display: 'flex',
            gap: '16px',
            fontSize: '12px',
            color: '#94a3b8',
          }}
        >
          <span>🟢 Mũi tên: chặn → bị chặn</span>
          <span>🟢 Xanh: Done · 🟡 Vàng: In Progress · 🔴 Đỏ: Overdue</span>
          <span>🖱️ Click node: mở chi tiết</span>
        </div>
      </div>
    </div>
  );
};

export default DependencyGraphModal;

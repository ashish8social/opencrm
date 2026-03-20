import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { DndContext, DragEndEvent, DragOverlay, DragStartEvent, PointerSensor, useSensor, useSensors } from '@dnd-kit/core';
import { recordApi } from '../api/recordApi';
import { metadataApi } from '../api/metadataApi';
import { CrmRecord } from '../types/record';

const STAGES = [
  'Prospecting', 'Qualification', 'Needs Analysis', 'Value Proposition',
  'Proposal/Price Quote', 'Negotiation/Review', 'Closed Won', 'Closed Lost',
];

const STAGE_COLORS: Record<string, string> = {
  'Prospecting': 'bg-blue-50 border-blue-200',
  'Qualification': 'bg-indigo-50 border-indigo-200',
  'Needs Analysis': 'bg-violet-50 border-violet-200',
  'Value Proposition': 'bg-purple-50 border-purple-200',
  'Proposal/Price Quote': 'bg-fuchsia-50 border-fuchsia-200',
  'Negotiation/Review': 'bg-amber-50 border-amber-200',
  'Closed Won': 'bg-green-50 border-green-200',
  'Closed Lost': 'bg-red-50 border-red-200',
};

function formatCurrency(val: number | undefined) {
  if (!val) return '$0';
  if (val >= 1000000) return `$${(val / 1000000).toFixed(1)}M`;
  if (val >= 1000) return `$${(val / 1000).toFixed(0)}K`;
  return `$${val.toFixed(0)}`;
}

export function PipelineBoard() {
  const queryClient = useQueryClient();
  const [activeCard, setActiveCard] = useState<CrmRecord | null>(null);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } })
  );

  const { data: records, isLoading } = useQuery({
    queryKey: ['records', 'Opportunity', 'all'],
    queryFn: () => recordApi.list('Opportunity', { size: 200 }),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, stage }: { id: string; stage: string }) =>
      recordApi.update('Opportunity', id, { Stage: stage }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['records', 'Opportunity'] });
      queryClient.invalidateQueries({ queryKey: ['pipeline-summary'] });
    },
  });

  const opportunities = records?.content ?? [];

  const byStage = STAGES.reduce<Record<string, CrmRecord[]>>((acc, stage) => {
    acc[stage] = opportunities.filter(r => String(r.data['Stage'] ?? '') === stage);
    return acc;
  }, {});

  const handleDragStart = (event: DragStartEvent) => {
    const opp = opportunities.find(r => r.id === event.active.id);
    if (opp) setActiveCard(opp);
  };

  const handleDragEnd = (event: DragEndEvent) => {
    setActiveCard(null);
    const { active, over } = event;
    if (!over) return;

    const targetStage = over.id as string;
    const opp = opportunities.find(r => r.id === active.id);
    if (opp && String(opp.data['Stage'] ?? '') !== targetStage) {
      updateMutation.mutate({ id: opp.id, stage: targetStage });
    }
  };

  if (isLoading) {
    return <div className="text-center py-12 text-gray-500">Loading pipeline...</div>;
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Pipeline Board</h1>
        <Link
          to="/o/Opportunity/new"
          className="px-4 py-2 bg-primary-600 text-white text-sm rounded-md hover:bg-primary-700"
        >
          New Opportunity
        </Link>
      </div>

      <DndContext sensors={sensors} onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
        <div className="flex gap-3 overflow-x-auto pb-4" style={{ minHeight: '70vh' }}>
          {STAGES.map(stage => {
            const stageOpps = byStage[stage] || [];
            const totalAmount = stageOpps.reduce((sum, r) => sum + (Number(r.data['Amount']) || 0), 0);
            return (
              <StageColumn
                key={stage}
                stage={stage}
                opportunities={stageOpps}
                totalAmount={totalAmount}
              />
            );
          })}
        </div>

        <DragOverlay>
          {activeCard ? <OpportunityCard opportunity={activeCard} isDragging /> : null}
        </DragOverlay>
      </DndContext>
    </div>
  );
}

function StageColumn({ stage, opportunities, totalAmount }: {
  stage: string;
  opportunities: CrmRecord[];
  totalAmount: number;
}) {
  const { setNodeRef, isOver } = useDroppable({ id: stage });
  const colorClass = STAGE_COLORS[stage] || 'bg-gray-50 border-gray-200';

  return (
    <div
      ref={setNodeRef}
      className={`flex-shrink-0 w-64 rounded-lg border-2 ${colorClass} ${isOver ? 'ring-2 ring-primary-400' : ''} flex flex-col`}
    >
      <div className="p-3 border-b border-inherit">
        <h3 className="text-sm font-semibold text-gray-800 truncate">{stage}</h3>
        <div className="flex items-center justify-between mt-1">
          <span className="text-xs text-gray-500">{opportunities.length} deal{opportunities.length !== 1 ? 's' : ''}</span>
          <span className="text-xs font-medium text-gray-700">{formatCurrency(totalAmount)}</span>
        </div>
      </div>
      <div className="p-2 flex-1 space-y-2 overflow-y-auto">
        {opportunities.map(opp => (
          <DraggableCard key={opp.id} opportunity={opp} />
        ))}
      </div>
    </div>
  );
}

function DraggableCard({ opportunity }: { opportunity: CrmRecord }) {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({ id: opportunity.id });

  const style = transform ? {
    transform: `translate(${transform.x}px, ${transform.y}px)`,
  } : undefined;

  return (
    <div ref={setNodeRef} style={style} {...attributes} {...listeners}>
      <OpportunityCard opportunity={opportunity} isDragging={isDragging} />
    </div>
  );
}

function OpportunityCard({ opportunity, isDragging }: { opportunity: CrmRecord; isDragging?: boolean }) {
  const amount = Number(opportunity.data['Amount']) || 0;
  const closeDate = opportunity.data['CloseDate'] as string | undefined;

  return (
    <div className={`bg-white rounded-md border border-gray-200 p-3 cursor-grab ${isDragging ? 'opacity-50 shadow-lg' : 'hover:shadow-sm'}`}>
      <Link
        to={`/o/Opportunity/${opportunity.id}`}
        className="text-sm font-medium text-primary-600 hover:text-primary-800 block truncate"
        onClick={e => e.stopPropagation()}
      >
        {opportunity.name || '(No Name)'}
      </Link>
      <div className="mt-1 flex items-center justify-between">
        <span className="text-xs font-medium text-gray-700">{formatCurrency(amount)}</span>
        {closeDate && (
          <span className="text-xs text-gray-400">{closeDate}</span>
        )}
      </div>
    </div>
  );
}

// Inline droppable/draggable hooks to avoid extra imports
import { useDroppable, useDraggable } from '@dnd-kit/core';

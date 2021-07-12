import { GroupBy } from '../types';
import { FunctionComponent } from 'react';
import { InlineFormLabel, SegmentInput } from '@grafana/ui';
import React from 'react';

export interface Props {
  groupBy: GroupBy;
  onChange: (groupBy: GroupBy) => void;
  isPoint: boolean;
}

export const GroupByLabel: FunctionComponent<Props> = ({ groupBy, onChange, isPoint }) => (
  <>
    {
      <>
        <SegmentInput
          value={groupBy.samplingInterval}
          onChange={(string) => onChange({ ...groupBy, samplingInterval: string.toString() })}
          className="width-5"
          placeholder="1s"
        />
        <InlineFormLabel className="query-keyword" width={11}>
          sliding step
        </InlineFormLabel>
        <SegmentInput
          className="width-5"
          placeholder="(optional)"
          value={groupBy.step}
          onChange={(string) => onChange({ ...groupBy, step: string.toString() })}
        />
      </>
    }
  </>
);

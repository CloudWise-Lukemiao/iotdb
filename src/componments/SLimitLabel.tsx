import { LimitAll } from '../types';
import { FunctionComponent } from 'react';
import { InlineFormLabel, SegmentInput } from '@grafana/ui';
import React from 'react';

export interface Props {
  limitAll: LimitAll;
  onChange: (limitStr: LimitAll) => void;
}

export const SLimitLabel: FunctionComponent<Props> = ({ limitAll, onChange }) => (
  <>
    {
      <>
        <SegmentInput
          className="width-5"
          placeholder="(optional)"
          value={limitAll.limit}
          onChange={(string) => onChange({ ...limitAll, limit: string.toString() })}
        />
        <InlineFormLabel className="query-keyword" width={11}>
          slimit
        </InlineFormLabel>
        <SegmentInput
          className="width-5"
          placeholder="(optional)"
          value={limitAll.slimit}
          onChange={(string) => onChange({ ...limitAll, slimit: string.toString() })}
        />
      </>
    }
  </>
);

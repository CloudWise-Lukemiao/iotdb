/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Fill } from '../types';
import React, { FunctionComponent } from 'react';
import { Icon, Segment, SegmentInput } from '@grafana/ui';
import { toOption } from '../functions';
import { SelectableValue } from '@grafana/data';

export interface Props {
  onChange: (fillClauses: Fill[]) => void;
  fillClauses: Fill[];
}

const removeText = '-- remove stat --';
const removeOption: SelectableValue<string> = { label: removeText, value: removeText };

const previous = ['previous', 'previousUntilLast'];

const dataTypes = ['INT32', 'INT64', 'FLOAT', 'DOUBLE', 'BOOLEAN', 'TEXT', 'ALL'];

export const FillClause: FunctionComponent<Props> = ({ fillClauses, onChange }) => (
  <>
    {fillClauses &&
      fillClauses.map((value, index) => (
        <>
          <Segment
            onChange={({ value: value = '' }) => {
              if (value === removeText) {
                onChange(fillClauses.filter((_, i) => i !== index));
              } else {
                onChange(fillClauses.map((v, i) => (i === index ? { ...v, dataType: value } : v)));
              }
            }}
            options={[...dataTypes.map(toOption), removeOption]}
            value={value.dataType}
            className="width-5"
          />
          <Segment
            onChange={({ value: value = '' }) => {
              if (value === removeText) {
                onChange(fillClauses.filter((_, i) => i !== index));
              } else {
                onChange(fillClauses.map((v, i) => (i === index ? { ...v, previous: value } : v)));
              }
            }}
            options={[...previous.map(toOption), removeOption]}
            value={value.previous}
          />
          <SegmentInput
            onChange={(value) => {
              onChange(fillClauses.map((v, i) => (i === index ? { ...v, duration: value.toString() } : v)));
            }}
            value={value.duration}
            className="width-4"
          />
        </>
      ))}
    {fillClauses.length < 1 && (
      <Segment
        allowCustomValue={false}
        Component={
          <a className="gf-form-label query-part">
            <Icon name="plus" />
          </a>
        }
        onChange={(item: SelectableValue<string>) => {
          let itemString = '';
          if (item.value) {
            itemString = item.value;
          }
          dataTypes.filter((d) => d.includes(itemString));
          onChange([...fillClauses, { dataType: itemString, previous: previous[0], duration: '' }]);
        }}
        options={dataTypes.map(toOption)}
        className="width-5"
      />
    )}
  </>
);

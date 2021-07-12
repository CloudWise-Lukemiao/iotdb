import { DataQuery, DataSourceJsonData } from '@grafana/data';

export interface MyQuery extends DataQuery {
  paths: string[];
  aggregation?: string;
  fills?: Fill[];
  groupBy?: GroupBy;
  stime: number;
  etime: number;
  limitAll?: LimitAll;
}

export interface GroupBy {
  step: string;
  samplingInterval: string;
}

export interface Fill {
  dataType: string;
  previous: string;
  duration: string;
}

export interface LimitAll {
  slimit: string;
  limit: string;
}
/**
 * These are options configured for each DataSource instance
 */
export interface MyDataSourceOptions extends DataSourceJsonData {
  url: string;
  password: string;
  username: string;
}

/**
 * Value that is used in the backend, but never sent over HTTP to the frontend
 */
export interface MySecureJsonData {
  apiKey?: string;
}

export interface Col {
  name: string;
  values: [];
}

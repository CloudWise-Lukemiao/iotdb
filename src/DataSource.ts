import { getBackendSrv } from '@grafana/runtime';
import {
  DataQueryRequest,
  DataQueryResponse,
  DataSourceApi,
  DataSourceInstanceSettings,
  MetricFindValue,
  toDataFrame,
} from '@grafana/data';

import { MyDataSourceOptions, MyQuery } from './types';
import { toMetricFindValue } from './functions';
import Base64 from 'crypto-js/enc-base64';
import Utf8 from 'crypto-js/enc-utf8';
export class DataSource extends DataSourceApi<MyQuery, MyDataSourceOptions> {
  username: string;
  password: string;
  url: string;

  constructor(instanceSettings: DataSourceInstanceSettings<MyDataSourceOptions>) {
    super(instanceSettings);
    this.url = instanceSettings.jsonData.url;
    this.password = instanceSettings.jsonData.password;
    this.username = instanceSettings.jsonData.username;
  }

  async query(options: DataQueryRequest<MyQuery>): Promise<DataQueryResponse> {
    const { range } = options;
    const dataFrames = options.targets.map((target) => {
      target.stime = range!.from.valueOf();
      target.etime = range!.to.valueOf();
      target.paths = ['root', ...target.paths];
      return this.doRequest(target);
    });
    return Promise.all(dataFrames)
      .then((a) => a.reduce((accumulator, value) => accumulator.concat(value), []))
      .then((data) => ({ data }));
    // const { range } = options;
    // const from = range!.from.valueOf();
    // const to = range!.to.valueOf();
    //
    // // Return a constant for each query.
    // const data = options.targets.map(target => {
    //   return new MutableDataFrame({
    //     refId: target.refId,
    //     fields: [
    //       { name: 'Time', values: [from, to], type: FieldType.time },
    //       { name: 'Value', values: [1, 1], type: FieldType.number },
    //     ],
    //   });
    // });
    //
    // return { data };
  }

  async doRequest(query: MyQuery) {
    const myHeader = new Headers();
    myHeader.append('Content-Type', 'application/json');
    const Authorization = 'Basic ' + Base64.stringify(Utf8.parse(this.username + ':' + this.password));
    myHeader.append('Authorization', Authorization);
    return await getBackendSrv()
      .datasourceRequest({
        method: 'POST',
        url: this.url + '/v1/grafana/query/json',
        data: JSON.stringify(query),
        headers: myHeader,
      })
      .then((response) => response.data)
      .then((a) => {
        if (a instanceof Array) {
          return a.map(toDataFrame);
        } else {
          throw 'the result is not array';
        }
      });
  }

  metricFindQuery(query: any, options?: any): Promise<MetricFindValue[]> {
    return this.getChildPaths(query);
  }

  async getChildPaths(detachedPath: string[]) {
    const myHeader = new Headers();
    myHeader.append('Content-Type', 'application/json');
    const Authorization = 'Basic ' + Base64.stringify(Utf8.parse(this.username + ':' + this.password));
    myHeader.append('Authorization', Authorization);
    const prefixPath: string = detachedPath.reduce((a, b) => a + '.' + b);
    console.log(prefixPath);
    return await getBackendSrv()
      .datasourceRequest({
        method: 'POST',
        url: this.url + '/v1/grafana/node',
        data: detachedPath,
        headers: myHeader,
      })
      .then((response) => {
        if (response.data instanceof Array) {
          return response.data;
        } else {
          throw 'the result is not array';
        }
      })
      .then((data) => data.map(toMetricFindValue));
  }

  async testDatasource() {
    const myHeader = new Headers();
    myHeader.append('Content-Type', 'application/json');
    const Authorization = 'Basic ' + Base64.stringify(Utf8.parse(this.username + ':' + this.password));
    myHeader.append('Authorization', Authorization);
    const response = getBackendSrv().datasourceRequest({
      url: this.url + '/ping',
      method: 'GET',
      headers: myHeader,
    });
    let status = '';
    let message = '';
    await response.then((res) => {
      let b = res.data.code === 4 ? true : false;
      if (b) {
        status = 'success';
        message = res.data.message;
      } else {
        status = 'error';
        message = res.data.message;
      }
    });
    return {
      status: status,
      message: message,
    };
  }
}

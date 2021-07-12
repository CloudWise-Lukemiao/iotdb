import { MetricFindValue, SelectableValue } from '@grafana/data';

export const toOption = (value: string) => ({ label: value, value } as SelectableValue<string>);

//export const toMetricFindValue = (value: string) => ({ text: value } as MetricFindValue);
// export const toMetricFindValue = (series: JSON) => {
//   console.log(series);
//   (series.leaf ? {} : { text: jsonseries.name }) as MetricFindValue;
// };
export const toMetricFindValue = (data: any) => ({ text: data } as MetricFindValue);

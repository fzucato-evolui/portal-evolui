export enum BucketFileTypeEnum {
  BUCKET = "BUCKET",
  DIRECTORY = "DIRECTORY",
  FILE = "FILE"
}
export class BucketModel {
  name: string;
  type: BucketFileTypeEnum;
  path: string;
  arn: string | null;
  account: any;
}

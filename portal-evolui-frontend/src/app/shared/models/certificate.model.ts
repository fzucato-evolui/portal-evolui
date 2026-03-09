export enum CertificateTypeEnum {
  JKS= "JKS",
  PKCS12 = "PKCS12"
}

export class CertificateModel {
  public certificateType: CertificateTypeEnum;
  public file: [];
  public fileName: string;
  public password: string;
}

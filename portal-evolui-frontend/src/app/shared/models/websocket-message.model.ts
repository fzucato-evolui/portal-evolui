export class WebsocketMessageModel {
  public from: string;
  public to: string;
  public message: any;
  public error: string;
}

export class ConsoleResponseMessageModel {
  public currentDirectory: string;
  public finished: boolean;
  public output: string;
  public outputError: string;
  public sequence: number;
}

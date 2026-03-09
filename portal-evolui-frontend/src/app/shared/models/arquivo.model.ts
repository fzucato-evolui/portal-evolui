import {MediaType} from "./file.model";

export class ArquivoModel {
    public id: number;
    public arquivo: string;
    public titulo: string;
    public descricao: string;
    public ordem: any;
    public extensao: string;
    public tipo: MediaType;
    public nome_arquivo: string;
    public tipo_midia: string;
    public link: string;
    public tempFile: string;
    public base64: string;
}
import {Observable, Subject} from "rxjs";
import moment from 'moment';

export class UtilFunctions {

  public static validarCPF(strCPF: string): boolean {
    let soma = 0;

    strCPF = UtilFunctions.removeNonNumbers(strCPF);

    if (strCPF.length !== 11) {
      return false;
    }

    if (/^(.)\1*$/.test(strCPF)) {
      return false;
    }

    soma = this.soma(soma, strCPF, 11, 9);

    let resto = (soma * 10) % 11;

    if (resto === 10 || resto === 11) {
      resto = 0;
    }

    if (resto !== parseInt(strCPF.substring(9, 10), 10)) {
      return false;
    }

    soma = 0;
    soma = this.soma(soma, strCPF, 12, 10);

    resto = (soma * 10) % 11;

    if (resto === 10 || resto === 11) {
      resto = 0;
    }

    return resto === parseInt(strCPF.substring(10, 11), 10);

  }

  public static validarCNPJ(cnpj: string) : boolean {

    cnpj = UtilFunctions.removeNonNumbers(cnpj);

    if (cnpj.length !== 14) {
      return false;
    }
    if (/^([0-9])\1*$/.test(cnpj)) {
      return false;
    }

    // Valida DVs
    let tamanho = cnpj.length - 2;
    let numeros = cnpj.substring(0, tamanho);
    const digitos = cnpj.substring(tamanho);
    let soma = 0;
    let pos = tamanho - 7;
    for (let i = tamanho; i >= 1; i--) {
      soma += parseInt(numeros.charAt(tamanho - i), 10) * pos--;
      if (pos < 2) {
        pos = 9;
      }
    }
    let resultado = soma % 11 < 2 ? 0 : 11 - soma % 11;
    if (resultado.toString() !== digitos.charAt(0)) {
      return false;
    }

    tamanho = tamanho + 1;
    numeros = cnpj.substring(0, tamanho);
    soma = 0;
    pos = tamanho - 7;
    for (let i = tamanho; i >= 1; i--) {
      soma += parseInt(numeros.charAt(tamanho - i), 10) * pos--;
      if (pos < 2) {
        pos = 9;
      }
    }
    resultado = soma % 11 < 2 ? 0 : 11 - soma % 11;
    if (resultado.toString() !== digitos.charAt(1)) {
      return false;
    }

    return true;

  }

  public static validarDocumento(documento: string) : boolean {
    documento = UtilFunctions.removeNonNumbers(documento);
    if (documento.length === 14) {
      return UtilFunctions.validarCNPJ(documento);
    } else {
      return UtilFunctions.validarCPF(documento);
    }
  }

  private static soma(soma: number, cpf: string, digito: number, length: number) {
    for (let index = 1; index <= length; index++) {
      soma = soma + parseInt(cpf.substring(index - 1, index), 10) * (digito - index);
    }
    return soma;
  }

  public static removeNonAlphaNumeric(value: string): string {
    if (!UtilFunctions.isValidStringOrArray(value)) {
      return '';
    }
    const regExp = /[^A-Za-z0-9]/g;
    return value.replace(regExp, '');
  }

  public static removeNonNumbers(value: string): string {
    if (!UtilFunctions.isValidStringOrArray(value)) {
      return '';
    }
    const regExp = /[^0-9]/g;
    return value.replace(regExp, '');
  }

  public static removeAccents(value: string): string {
    if (!UtilFunctions.isValidStringOrArray(value)) {
      return '';
    }
    return value.normalize('NFD').replace(/[\u0300-\u036f]/g, '');
  }

  public static removeNonPrintable(value: string): string {
    if (!UtilFunctions.isValidStringOrArray(value)) {
      return '';
    }
    var re = /[\0-\x1F\x7F-\x9F\xAD\u0378\u0379\u037F-\u0383\u038B\u038D\u03A2\u0528-\u0530\u0557\u0558\u0560\u0588\u058B-\u058E\u0590\u05C8-\u05CF\u05EB-\u05EF\u05F5-\u0605\u061C\u061D\u06DD\u070E\u070F\u074B\u074C\u07B2-\u07BF\u07FB-\u07FF\u082E\u082F\u083F\u085C\u085D\u085F-\u089F\u08A1\u08AD-\u08E3\u08FF\u0978\u0980\u0984\u098D\u098E\u0991\u0992\u09A9\u09B1\u09B3-\u09B5\u09BA\u09BB\u09C5\u09C6\u09C9\u09CA\u09CF-\u09D6\u09D8-\u09DB\u09DE\u09E4\u09E5\u09FC-\u0A00\u0A04\u0A0B-\u0A0E\u0A11\u0A12\u0A29\u0A31\u0A34\u0A37\u0A3A\u0A3B\u0A3D\u0A43-\u0A46\u0A49\u0A4A\u0A4E-\u0A50\u0A52-\u0A58\u0A5D\u0A5F-\u0A65\u0A76-\u0A80\u0A84\u0A8E\u0A92\u0AA9\u0AB1\u0AB4\u0ABA\u0ABB\u0AC6\u0ACA\u0ACE\u0ACF\u0AD1-\u0ADF\u0AE4\u0AE5\u0AF2-\u0B00\u0B04\u0B0D\u0B0E\u0B11\u0B12\u0B29\u0B31\u0B34\u0B3A\u0B3B\u0B45\u0B46\u0B49\u0B4A\u0B4E-\u0B55\u0B58-\u0B5B\u0B5E\u0B64\u0B65\u0B78-\u0B81\u0B84\u0B8B-\u0B8D\u0B91\u0B96-\u0B98\u0B9B\u0B9D\u0BA0-\u0BA2\u0BA5-\u0BA7\u0BAB-\u0BAD\u0BBA-\u0BBD\u0BC3-\u0BC5\u0BC9\u0BCE\u0BCF\u0BD1-\u0BD6\u0BD8-\u0BE5\u0BFB-\u0C00\u0C04\u0C0D\u0C11\u0C29\u0C34\u0C3A-\u0C3C\u0C45\u0C49\u0C4E-\u0C54\u0C57\u0C5A-\u0C5F\u0C64\u0C65\u0C70-\u0C77\u0C80\u0C81\u0C84\u0C8D\u0C91\u0CA9\u0CB4\u0CBA\u0CBB\u0CC5\u0CC9\u0CCE-\u0CD4\u0CD7-\u0CDD\u0CDF\u0CE4\u0CE5\u0CF0\u0CF3-\u0D01\u0D04\u0D0D\u0D11\u0D3B\u0D3C\u0D45\u0D49\u0D4F-\u0D56\u0D58-\u0D5F\u0D64\u0D65\u0D76-\u0D78\u0D80\u0D81\u0D84\u0D97-\u0D99\u0DB2\u0DBC\u0DBE\u0DBF\u0DC7-\u0DC9\u0DCB-\u0DCE\u0DD5\u0DD7\u0DE0-\u0DF1\u0DF5-\u0E00\u0E3B-\u0E3E\u0E5C-\u0E80\u0E83\u0E85\u0E86\u0E89\u0E8B\u0E8C\u0E8E-\u0E93\u0E98\u0EA0\u0EA4\u0EA6\u0EA8\u0EA9\u0EAC\u0EBA\u0EBE\u0EBF\u0EC5\u0EC7\u0ECE\u0ECF\u0EDA\u0EDB\u0EE0-\u0EFF\u0F48\u0F6D-\u0F70\u0F98\u0FBD\u0FCD\u0FDB-\u0FFF\u10C6\u10C8-\u10CC\u10CE\u10CF\u1249\u124E\u124F\u1257\u1259\u125E\u125F\u1289\u128E\u128F\u12B1\u12B6\u12B7\u12BF\u12C1\u12C6\u12C7\u12D7\u1311\u1316\u1317\u135B\u135C\u137D-\u137F\u139A-\u139F\u13F5-\u13FF\u169D-\u169F\u16F1-\u16FF\u170D\u1715-\u171F\u1737-\u173F\u1754-\u175F\u176D\u1771\u1774-\u177F\u17DE\u17DF\u17EA-\u17EF\u17FA-\u17FF\u180F\u181A-\u181F\u1878-\u187F\u18AB-\u18AF\u18F6-\u18FF\u191D-\u191F\u192C-\u192F\u193C-\u193F\u1941-\u1943\u196E\u196F\u1975-\u197F\u19AC-\u19AF\u19CA-\u19CF\u19DB-\u19DD\u1A1C\u1A1D\u1A5F\u1A7D\u1A7E\u1A8A-\u1A8F\u1A9A-\u1A9F\u1AAE-\u1AFF\u1B4C-\u1B4F\u1B7D-\u1B7F\u1BF4-\u1BFB\u1C38-\u1C3A\u1C4A-\u1C4C\u1C80-\u1CBF\u1CC8-\u1CCF\u1CF7-\u1CFF\u1DE7-\u1DFB\u1F16\u1F17\u1F1E\u1F1F\u1F46\u1F47\u1F4E\u1F4F\u1F58\u1F5A\u1F5C\u1F5E\u1F7E\u1F7F\u1FB5\u1FC5\u1FD4\u1FD5\u1FDC\u1FF0\u1FF1\u1FF5\u1FFF\u200B-\u200F\u202A-\u202E\u2060-\u206F\u2072\u2073\u208F\u209D-\u209F\u20BB-\u20CF\u20F1-\u20FF\u218A-\u218F\u23F4-\u23FF\u2427-\u243F\u244B-\u245F\u2700\u2B4D-\u2B4F\u2B5A-\u2BFF\u2C2F\u2C5F\u2CF4-\u2CF8\u2D26\u2D28-\u2D2C\u2D2E\u2D2F\u2D68-\u2D6E\u2D71-\u2D7E\u2D97-\u2D9F\u2DA7\u2DAF\u2DB7\u2DBF\u2DC7\u2DCF\u2DD7\u2DDF\u2E3C-\u2E7F\u2E9A\u2EF4-\u2EFF\u2FD6-\u2FEF\u2FFC-\u2FFF\u3040\u3097\u3098\u3100-\u3104\u312E-\u3130\u318F\u31BB-\u31BF\u31E4-\u31EF\u321F\u32FF\u4DB6-\u4DBF\u9FCD-\u9FFF\uA48D-\uA48F\uA4C7-\uA4CF\uA62C-\uA63F\uA698-\uA69E\uA6F8-\uA6FF\uA78F\uA794-\uA79F\uA7AB-\uA7F7\uA82C-\uA82F\uA83A-\uA83F\uA878-\uA87F\uA8C5-\uA8CD\uA8DA-\uA8DF\uA8FC-\uA8FF\uA954-\uA95E\uA97D-\uA97F\uA9CE\uA9DA-\uA9DD\uA9E0-\uA9FF\uAA37-\uAA3F\uAA4E\uAA4F\uAA5A\uAA5B\uAA7C-\uAA7F\uAAC3-\uAADA\uAAF7-\uAB00\uAB07\uAB08\uAB0F\uAB10\uAB17-\uAB1F\uAB27\uAB2F-\uABBF\uABEE\uABEF\uABFA-\uABFF\uD7A4-\uD7AF\uD7C7-\uD7CA\uD7FC-\uF8FF\uFA6E\uFA6F\uFADA-\uFAFF\uFB07-\uFB12\uFB18-\uFB1C\uFB37\uFB3D\uFB3F\uFB42\uFB45\uFBC2-\uFBD2\uFD40-\uFD4F\uFD90\uFD91\uFDC8-\uFDEF\uFDFE\uFDFF\uFE1A-\uFE1F\uFE27-\uFE2F\uFE53\uFE67\uFE6C-\uFE6F\uFE75\uFEFD-\uFF00\uFFBF-\uFFC1\uFFC8\uFFC9\uFFD0\uFFD1\uFFD8\uFFD9\uFFDD-\uFFDF\uFFE7\uFFEF-\uFFFB\uFFFE\uFFFF]/g;
    return value.replace(re, "");
  }

  public static formatDateDatabase(d: Date, includeTime: boolean) {
    const year = '' + d.getFullYear();
    let month = '' + (d.getMonth() + 1),
      day = '' + d.getDate(),
      hour = '' + d.getHours(),
      minute = '' + d.getMinutes(),
      seconds = '' + d.getSeconds();

    if (month.length < 2) {
      month = '0' + month;
    }
    if (day.length < 2) {
      day = '0' + day;
    }
    if (hour.length < 2) {
      hour = '0' + hour;
    }
    if (minute.length < 2) {
      minute = '0' + minute;
    }
    if (seconds.length < 2) {
      seconds = '0' + seconds;
    }

    let formatedDate: string = year + '-' + month + '-' + day;
    if (includeTime) {
      formatedDate += ' ' + hour + ':' + minute + ':' + seconds;
    }
    return formatedDate;
  }

  public static formatDateBrazil(date: Date, includeTime: boolean) {
    if (!UtilFunctions.isValidObject(date)) {
      return undefined;
    }
    const year = '' + date.getFullYear();
    let month = '' + (date.getMonth() + 1),
      day = '' + date.getDate(),
      hour = '' + date.getHours(),
      minute = '' + date.getMinutes(),
      seconds = '' + date.getSeconds();

    if (month.length < 2) {
      month = '0' + month;
    }
    if (day.length < 2) {
      day = '0' + day;
    }
    if (hour.length < 2) {
      hour = '0' + hour;
    }
    if (minute.length < 2) {
      minute = '0' + minute;
    }
    if (seconds.length < 2) {
      seconds = '0' + seconds;
    }

    let formatedDate: string = day + '/' + month + '/' + year;
    if (includeTime) {
      formatedDate += ' ' + hour + ':' + minute + ':' + seconds;
    }
    return formatedDate;
  }

  public static formatDateOnlyNumbers(d: Date, includeTime: boolean) {
    const formated = UtilFunctions.formatDateDatabase(d, includeTime);
    return UtilFunctions.removeNonNumbers(formated);
  }

  public static parseDateDataBaseFormat(dateString: string): Date {
    const searchValue = /-/g;
    return new Date(dateString.replace(searchValue, '/'));
  }

  public static roundNumber(value: number): number {
    if (isNaN(value)) {
      return null;
    }
    value = Math.round(value * 100) / 100;
    return value;
  }

  public static isValidStringOrArray(value: any): boolean {
    if (value !== null && value !== undefined) {
      if (Array.isArray(value) === true) {
        return value.length > 0;
      } else {
        return value.toString().length > 0
      }
    }

    return false;
  }

  public static isValidObject(value: any): boolean {
    return !!(value !== null && value !== undefined && value);

  }

  public static isValidEmail(value: string) {
    return UtilFunctions.isValidStringOrArray(value) && /^\w+([\.-]?\w+)*@\w+([\.-]?\w+)*(\.\w{2,3})+$/.test(value);
  }

  public static isValidURL(value: string) {

    const regex = new RegExp("^(http[s]?:\\/\\/(www\\.)?|ftp:\\/\\/(www\\.)?|www\\.){1}([0-9A-Za-z-\\.@:%_\+~#=]+)+((\\.[a-zA-Z]{2,3})+)(/(.)*)?(\\?(.)*)?");
    return UtilFunctions.isValidStringOrArray(value) && regex.test(value);
  }
  public static arrayBufferToBase64(buffer) {
    let binary = '';
    const bytes = new Uint8Array(buffer);
    const len = bytes.byteLength;
    for (let i = 0; i < len; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return btoa(binary);
  }

  public static arrayBufferToString(buffer) {
    let text = '';
    const bytes = new Uint8Array(buffer);
    return new TextDecoder().decode(bytes);;
  }

  public static arrayBufferToByteArray(buffer: ArrayBuffer) {
    const uintArr = new Uint8Array(buffer);
    return  Array.from(uintArr);
  }

  public static convertBase64ToBlobData(base64Data: string, contentType: string, sliceSize = 512) {
    base64Data = base64Data.replace(/\s/g, '');
    const byteCharacters = atob(base64Data);
    const byteArrays = [];

    for (let offset = 0; offset < byteCharacters.length; offset += sliceSize) {
      const slice = byteCharacters.slice(offset, offset + sliceSize);

      const byteNumbers = new Array(slice.length);
      for (let i = 0; i < slice.length; i++) {
        byteNumbers[i] = slice.charCodeAt(i);
      }

      const byteArray = new Uint8Array(byteNumbers);

      byteArrays.push(byteArray);
    }

    const blob = new Blob(byteArrays, { type: contentType });
    return blob;
  }

  public static getBrowserName() {
    const agent = window.navigator.userAgent.toLowerCase();
    switch (true) {
      case agent.indexOf('edge') > -1:
        return 'edge';
      case agent.indexOf('opr') > -1 && !!(<any>window).opr:
        return 'opera';
      case agent.indexOf('chrome') > -1 && !!(<any>window).chrome:
        return 'chrome';
      case agent.indexOf('trident') > -1:
        return 'ie';
      case agent.indexOf('firefox') > -1:
        return 'firefox';
      case agent.indexOf('safari') > -1:
        return 'safari';
      default:
        return 'other';
    }
  }

  public static formatarDocumento(documento: string) {
    if (!UtilFunctions.isValidStringOrArray(documento)) {
      return '';
    }
    documento = UtilFunctions.removeNonNumbers(documento);
    if (documento.length === 11) {
      return documento.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/g,'\$1.\$2.\$3\-\$4');
    } else if (documento.length === 14) {
      return documento.replace(/(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})/g,'\$1.\$2.\$3\/\$4\-\$5');
    }
    return '';
  }

  public static getNomeMes(mes: number) {
    const date = new Date();
    date.setMonth(mes);
    const month = date.toLocaleString('default', { month: 'long' });
    return month;
  }

  public static toTitleCase(str): string {
    str = str.toLowerCase().split(' ');
    for (let i = 0; i < str.length; i++) {
      str[i] = str[i].charAt(0).toUpperCase() + str[i].slice(1);
    }
    return str.join(' ');
  }

  public static getEnumKeyByEnumValue(myEnum, enumValue) {
    for (const key of Object.keys(myEnum)) {
      const value: string = myEnum[key];
      if (value === enumValue.toString()) {
        return key;
      }
    }
    return null;
  }

  public static getHttpErrorMessage(value) {
    let errorMessage;
    if (value.error.detail !== undefined) {
      errorMessage = value.error.detail;
    } else if (value.error && value.error.message) {
      errorMessage = value.error.message;
    } else if (value.error && value.error.error) {
      errorMessage = value.error.error;
    } else if (value.error && value.error.fieldErrors) {
      errorMessage = value.error.fieldErrors[0].message;
    } else if (value.error && value.error.message) {
      errorMessage = value.error.message;
    } else if (value.error) {
      errorMessage = value.error;
    } else if (value.message !== undefined) {
      errorMessage = value.message;
    } else {
      errorMessage = value;
    }
    if (errorMessage === null || errorMessage === undefined) {
      errorMessage = 'Erro desconhecido';
    }
    return errorMessage;
  }

  public static validarCep(value: string | null) {
    if (UtilFunctions.isValidStringOrArray(value) === true) {
      value = UtilFunctions.removeNonNumbers(value);
      return value.length === 8;
    }
    return false;
  }

  public static valueToDouble(num: any) {
    if (UtilFunctions.isValidStringOrArray(num) === true) {
      let numString = '' + num;
      const commaIndex = numString.lastIndexOf(',');
      const pointIndex = numString.lastIndexOf('.');
      if (commaIndex > pointIndex) {
        numString = numString.replace('.', '');
        numString = numString.replace(',', '.');
      } else if (pointIndex > commaIndex) {
        numString = numString.replace(',', '');
      }
      return parseFloat(numString);
      num = Number(num).toFixed(2);
      return num.toLocaleString('pt-BR').replace('.', ',').replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1.');
    }
    return 0.0;
  }

  public static formatNumber(num: any) {
    num = UtilFunctions.valueToDouble(num);
    num = Number(num).toFixed(2);
    return num.replace('.', ',').replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1.');
  }

  public static getFileExtension(fileName: string): string {
    if (UtilFunctions.isValidStringOrArray(fileName)) {
      const ext = fileName.split('.').pop();
      return '.' + ext;
    }
    return null;
  }
  public static removeFileExtension(filePath: string): string {
    return filePath.replace(/\.[^/.]+$/, ''); // Remove a última extensão
  }

  public static parseBoolean(value): boolean{
    switch(value){
      case true:
      case "true":
      case 1:
      case "1":
      case "on":
      case "yes":
        return true;
      default:
        return false;
    }
  }

  public static downLoadFile(data: any, type: string, fileName: string) {
    var blob = new Blob([data], { type: type.toString() });
    var url = window.URL.createObjectURL(blob);

    const a: HTMLAnchorElement = document.createElement('a') as HTMLAnchorElement;

    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();

    document.body.removeChild(a);
    URL.revokeObjectURL(url);

    /*
    var pwa = window.open(url);
    if (!pwa || pwa.closed || typeof pwa.closed == 'undefined') {
        alert('Please disable your Pop-up blocker and try again.');
    }
    */
  }

  public static urlToBlob(url): Promise<Blob>{
    return fetch(url).then(resp => {
      return resp.blob();
    });
  }

  public static getLocation(): Observable<GeolocationPosition | number> {

    let sendResult = new Subject<GeolocationPosition | number>();
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition((position: GeolocationPosition) => {
          if (position) {
            sendResult.next(position);
          } else {
            sendResult.next(-1);
          }
        },
        (error: GeolocationPositionError) => {
          sendResult.next(-1);
        });
    } else {
      sendResult.next(-1);
    }

    return sendResult.asObservable();
  }
  public static stringToDate (dateString: string, format: string): Date {
    const myMomentObject = moment(dateString, format);
    return myMomentObject.toDate();
  }
  public static dateToString (date: Date,format: string): string {
    return moment(date).format(format);
  }

  public static dateToMinutes (date: Date): number {
    var totalSeconds = UtilFunctions.dateToSeconds(date);
    var totalMinutes = parseInt(String(Math.floor(totalSeconds / 60)), 10);
    return totalMinutes;
  }

  public static dateToSeconds (date: Date): number {
    var totalSeconds = parseInt(String(Math.floor(date.getTime() / 1000)), 10);
    return totalSeconds;
  }

  public static bytes2Size(byteVal, includeSufix = true){
    var units=["Bytes", "KB", "MB", "GB", "TB"];
    if (includeSufix !== true) {
      units=["", "K", "M", "G", "T"];
    }
    var kounter=0;
    var kb= 1024;
    var div=byteVal/1;
    while(div>=kb){
      kounter++;
      div= div/kb;
    }
    return div.toFixed(1) + " " + units[kounter];
  }

  public static joinPaths(...paths: string[]): string {
    if (UtilFunctions.isValidStringOrArray(paths) === false) {
      return '';
    }
    return paths
      .map(path => path.replace(/(^\/+|\/+$)/g, '')) // Remove barras do início e fim
      .filter(path => path.length > 0) // Remove strings vazias, se tiver
      .join('/'); // Junta com uma única barra
  }

  public static arePathsEqual(path1: string, path2: string): boolean {
    const normalize = (path: string) => path.replace(/^\/+/, '').replace(/\/+$/, '');
    return normalize(path1) === normalize(path2);
  }

}

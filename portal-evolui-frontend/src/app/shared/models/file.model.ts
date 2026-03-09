import {UtilFunctions} from "../util/util-functions";

export type MediaType = 'AUDIO'| 'ZIP' | 'DOCUMENT' | 'IMAGE' | 'VIDEO' | 'UNKNOWN';

export class FileModel {
  public id: number;
  public key: string;
  public name: string;
  public title: string;
  public link: string;
  public description: string;
  public order: any;
  public extension: string;
  public fileType: MediaType;
  public contentType: string;
  public url: string;
  public size: number;
  public base64: string;
}

export class MimeModel {
  extension: string;
  documentoContentType: string;
  mediaType: MediaType
}

export class FileMimeModel extends MimeModel {
  size: number;
  base64: string;
  buffer: ArrayBuffer;
  fileName: string;
  url: string;

  public static update(model: FileMimeModel) {
    if (UtilFunctions.isValidStringOrArray(model.fileName) === true) {
      const m = MimeHelper.findByExtension(FileMimeModel.getFileExtension(model.fileName));
      if (UtilFunctions.isValidObject(m) === true) {
        model.extension = m.extension;
        model.documentoContentType = m.documentoContentType;
        model.mediaType = m.mediaType;
      }
    } else if (UtilFunctions.isValidStringOrArray(model.documentoContentType) === true) {
      const m = MimeHelper.findByContentType(model.documentoContentType);
      if (UtilFunctions.isValidObject(m) === true) {
        model.extension = m.extension;
        model.documentoContentType = m.documentoContentType;
        model.mediaType = m.mediaType;
      }
    }
    if (UtilFunctions.isValidStringOrArray(model.mediaType) === false) {
      const m = MimeHelper.mimes[0];
      model.extension = m.extension;
      model.documentoContentType = m.documentoContentType;
      model.mediaType = m.mediaType;
    }

    if (UtilFunctions.isValidObject(model.buffer) === true) {
      model.base64 = UtilFunctions.arrayBufferToBase64(model.buffer);
    }

  }

  public static getFileExtension(fileName: string): string {
    if (UtilFunctions.isValidStringOrArray(fileName)) {
      const ext = fileName.split('.').pop();
      return '.' + ext;
    }
    return null;
  }
}

export class MimeHelper {

  public static findByExtension(extension: string) : MimeModel {
    return MimeHelper.mimes.find(x => x.extension === extension.toLowerCase());
  }

  public static findByContentType(contentType: string) : MimeModel {
    return MimeHelper.mimes.find(x => x.documentoContentType === contentType);
  }

  public static getExtensionsAllowed(filter: {mediaTypes?:MediaType[], extentions?: string[], contentTypes?: string[]}): Array<string> {
    const exts: Array<string> = [];
    this.mimes.forEach(x => {
      if (UtilFunctions.isValidObject(filter)) {
        if (UtilFunctions.isValidStringOrArray(filter.mediaTypes)) {
          if (filter.mediaTypes.includes(x.mediaType)) {
            exts.push(x.extension);
          }
        }
        if (UtilFunctions.isValidStringOrArray(filter.extentions)) {
          if (filter.extentions.includes(x.extension)) {
            exts.push(x.extension);
          }
        }
        if (UtilFunctions.isValidStringOrArray(filter.contentTypes)) {
          if (filter.contentTypes.includes(x.documentoContentType)) {
            exts.push(x.extension);
          }
        }

      } else {
        exts.push(x.extension);
      }

    });
    return exts;
  }

  public static formatFileToShow(buffer: ArrayBuffer, model: MimeModel) {
    const base64 = "data:" + model.documentoContentType + ";base64," + UtilFunctions.arrayBufferToBase64(buffer);
    return base64;
  }

  static mimes: Array<MimeModel> = [
    {
      "extension": ".???",
      "documentoContentType": null,
      "mediaType": "UNKNOWN"
    },
    {
      "extension": ".m2a",
      "documentoContentType": "audio/mpeg",
      "mediaType": "AUDIO"
    },
    {
      "extension": ".aac",
      "documentoContentType": "audio/aac",
      "mediaType": "AUDIO"
    },
    {
      "extension": ".m3a",
      "documentoContentType": "audio/mpeg",
      "mediaType": "AUDIO"
    },
    {
      "extension": ".m3u",
      "documentoContentType": "audio/x-mpegurl",
      "mediaType": "AUDIO"
    },
    {
      "extension": ".mid",
      "documentoContentType": "audio/midi",
      "mediaType": "AUDIO"
    },
    {
      "extension": ".midi",
      "documentoContentType": "audio/midi",
      "mediaType": "AUDIO"
    },
    {
      "extension": ".mp2",
      "documentoContentType": "audio/mpeg",
      "mediaType": "AUDIO"
    },
    {
      "extension": ".mp2a",
      "documentoContentType": "audio/mpeg",
      "mediaType": "AUDIO"
    },
    {
      "extension": ".mp3",
      "documentoContentType": "audio/mpeg",
      "mediaType": "AUDIO"
    },
    {
      "extension": ".mp4a",
      "documentoContentType": "audio/mp4",
      "mediaType": "AUDIO"
    },
    {
      "extension": ".mpga",
      "documentoContentType": "audio/mpeg",
      "mediaType": "AUDIO"
    },
    {
      "extension": ".ogg",
      "documentoContentType": "audio/ogg",
      "mediaType": "AUDIO"
    },
    {
      "extension": ".wav",
      "documentoContentType": "audio/x-wav",
      "mediaType": "AUDIO"
    },
    {
      "extension": ".wax",
      "documentoContentType": "audio/x-ms-wax",
      "mediaType": "AUDIO"
    },
    {
      "extension": ".wma",
      "documentoContentType": "audio/x-ms-wma",
      "mediaType": "AUDIO"
    },
    {
      "extension": ".rar",
      "documentoContentType": "application/x-rar-compressed",
      "mediaType": "ZIP"
    },
    {
      "extension": ".zip",
      "documentoContentType": "application/zip",
      "mediaType": "ZIP"
    },
    {
      "extension": ".csv",
      "documentoContentType": "text/csv",
      "mediaType": "DOCUMENT"
    },
    {
      "extension": ".doc",
      "documentoContentType": "application/msword",
      "mediaType": "DOCUMENT"
    },
    {
      "extension": ".docx",
      "documentoContentType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      "mediaType": "DOCUMENT"
    },
    {
      "extension": ".json",
      "documentoContentType": "application/json",
      "mediaType": "DOCUMENT"
    },
    {
      "extension": ".log",
      "documentoContentType": "text/plain",
      "mediaType": "DOCUMENT"
    },
    {
      "extension": ".pdf",
      "documentoContentType": "application/pdf",
      "mediaType": "DOCUMENT"
    },
    {
      "extension": ".pps",
      "documentoContentType": "application/vnd.ms-powerpoint",
      "mediaType": "DOCUMENT"
    },
    {
      "extension": ".ppsx",
      "documentoContentType": "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
      "mediaType": "DOCUMENT"
    },
    {
      "extension": ".ppt",
      "documentoContentType": "application/vnd.ms-powerpoint",
      "mediaType": "DOCUMENT"
    },
    {
      "extension": ".pptx",
      "documentoContentType": "application/vnd.openxmlformats-officedocument.presentationml.presentation",
      "mediaType": "DOCUMENT"
    },
    {
      "extension": ".txt",
      "documentoContentType": "text/plain",
      "mediaType": "DOCUMENT"
    },
    {
      "extension": ".xls",
      "documentoContentType": "application/vnd.ms-excel",
      "mediaType": "DOCUMENT"
    },
    {
      "extension": ".xlsx",
      "documentoContentType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      "mediaType": "DOCUMENT"
    },
    {
      "extension": ".xml",
      "documentoContentType": "application/xml",
      "mediaType": "DOCUMENT"
    },
    {
      "extension": ".bmp",
      "documentoContentType": "image/bmp",
      "mediaType": "IMAGE"
    },
    {
      "extension": ".gif",
      "documentoContentType": "image/gif",
      "mediaType": "IMAGE"
    },
    {
      "extension": ".jpeg",
      "documentoContentType": "image/jpeg",
      "mediaType": "IMAGE"
    },
    {
      "extension": ".jpe",
      "documentoContentType": "image/jpeg",
      "mediaType": "IMAGE"
    },
    {
      "extension": ".jpg",
      "documentoContentType": "image/jpeg",
      "mediaType": "IMAGE"
    },
    {
      "extension": ".png",
      "documentoContentType": "image/png",
      "mediaType": "IMAGE"
    },
    {
      "extension": ".svg",
      "documentoContentType": "image/svg+xml",
      "mediaType": "IMAGE"
    },
    {
      "extension": ".tif",
      "documentoContentType": "image/tiff",
      "mediaType": "IMAGE"
    },
    {
      "extension": ".tiff",
      "documentoContentType": "image/tiff",
      "mediaType": "IMAGE"
    },
    {
      "extension": ".avi",
      "documentoContentType": "video/x-msvideo",
      "mediaType": "VIDEO"
    },
    {
      "extension": ".jpgm",
      "documentoContentType": "video/jpm",
      "mediaType": "VIDEO"
    },
    {
      "extension": ".jpgv",
      "documentoContentType": "video/jpeg",
      "mediaType": "VIDEO"
    },
    {
      "extension": ".jpm",
      "documentoContentType": "video/jpm",
      "mediaType": "VIDEO"
    },
    {
      "extension": ".m1v",
      "documentoContentType": "video/mpeg",
      "mediaType": "VIDEO"
    },
    {
      "extension": ".m2v",
      "documentoContentType": "video/mpeg",
      "mediaType": "VIDEO"
    },
    {
      "extension": ".m4u",
      "documentoContentType": "video/vnd.mpegurl",
      "mediaType": "VIDEO"
    },
    {
      "extension": ".3gpp",
      "documentoContentType": "video/3gpp",
      "mediaType": "VIDEO"
    },
    {
      "extension": ".m4v",
      "documentoContentType": "video/x-m4v",
      "mediaType": "VIDEO"
    },
    {
      "extension": ".mov",
      "documentoContentType": "video/quicktime",
      "mediaType": "VIDEO"
    },
    {
      "extension": ".movie",
      "documentoContentType": "video/x-sgi-movie",
      "mediaType": "VIDEO"
    },
    {
      "extension": ".mp4",
      "documentoContentType": "video/mp4",
      "mediaType": "VIDEO"
    },
    {
      "extension": ".mp4v",
      "documentoContentType": "video/mp4",
      "mediaType": "VIDEO"
    },
    {
      "extension": ".mpa",
      "documentoContentType": "video/mpeg",
      "mediaType": "VIDEO"
    },
    {
      "extension": ".mpe",
      "documentoContentType": "video/mpeg",
      "mediaType": "VIDEO"
    },
    {
      "extension": ".mpeg",
      "documentoContentType": "video/mpeg",
      "mediaType": "VIDEO"
    },
    {
      "extension": ".mpg",
      "documentoContentType": "video/mpeg",
      "mediaType": "VIDEO"
    },
    {
      "extension": ".mpg4",
      "documentoContentType": "video/mp4",
      "mediaType": "VIDEO"
    },
    {
      "extension": ".wm",
      "documentoContentType": "video/x-ms-wm",
      "mediaType": "VIDEO"
    },
    {
      "extension": ".wmv",
      "documentoContentType": "video/x-ms-wmv",
      "mediaType": "VIDEO"
    },
    {
      "extension": ".wmx",
      "documentoContentType": "video/x-ms-wmx",
      "mediaType": "VIDEO"
    },
    {
      "extension": ".wvx",
      "documentoContentType": "video/x-ms-wvx",
      "mediaType": "VIDEO"
    }
  ];
}

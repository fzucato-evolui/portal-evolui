export class FiltroModel {
    public operator: FiltroModelEnum;
    public logic: FiltroLogicModelEnum;
    public fieldName: string;
    public fieldValue: string[];
}

export enum FiltroModelEnum {
    EQUAL = 'EQUAL',
    NOT_EQUAL = 'NOT_EQUAL',
    GREATER_THAN = 'GREATER_THAN',
    LESS_THAN = 'LESS_THAN',
    GREATER_EQUAL = 'GREATER_EQUAL',
    LESS_EQUAL = 'LESS_EQUAL',
    BETWEEN = 'BETWEEN',
    LIKE = 'LIKE',
    NOT_LIKE = 'NOT_LIKE',
    IN = 'IN',
    NOT_IN = 'NOT_IN',
    IS_NULL = 'IS_NULL',
    IS_NOT_NULL = 'IS_NOT_NULL',
    HAVING = 'HAVING',
    HAVING_IN = 'HAVING_IN',
    ORDER_BY = 'ORDER_BY',
}

export enum FiltroLogicModelEnum {
    AND = 'AND',
    AND_GROUP_START = 'AND_GROUP_START',
    AND_GROUP_END = 'AND_GROUP_END',
    OR = 'OR',
    OR_GROUP_START = 'OR_GROUP',
    OR_GROUP_END = 'OR_GROUP_END',
}

export class IntervaloFiltroModel {
    public unidadeMedida: any;
    public inicial: any;
    public final: any;
}
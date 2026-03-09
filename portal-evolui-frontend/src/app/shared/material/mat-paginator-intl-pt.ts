import {MatPaginatorIntl} from '@angular/material/paginator';

export function getPortuguesePaginatorIntl(): MatPaginatorIntl {
    const paginatorIntl = new MatPaginatorIntl();

    paginatorIntl.itemsPerPageLabel = 'Itens por página:';
    paginatorIntl.nextPageLabel = 'Próxima página';
    paginatorIntl.previousPageLabel = 'Página anterior';
    paginatorIntl.firstPageLabel = 'Primeira página';
    paginatorIntl.lastPageLabel = 'Última página';

    paginatorIntl.getRangeLabel = (page: number, pageSize: number, length: number) => {
        if (length === 0 || pageSize === 0) {
            return `0 de ${length}`;
        }
        const startIndex = page * pageSize;
        const endIndex = Math.min(startIndex + pageSize, length);
        return `${startIndex + 1} - ${endIndex} de ${length}`;
    };

    return paginatorIntl;
}

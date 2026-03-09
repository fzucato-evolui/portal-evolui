import {AfterViewInit, Component, Input, ViewChild} from '@angular/core';
import {MatTableDataSource} from '@angular/material/table';
import {MatPaginator} from '@angular/material/paginator';
import {MatSort} from '@angular/material/sort';
import {Service} from '../../../../../../shared/models/health-checker.model';

@Component({
    selector: 'software-service',
    templateUrl: './software-service.component.html',
    styleUrls: ['./software-service.component.scss'],

    standalone: false
})
export class SoftwareServiceComponent implements AfterViewInit {

    private _data: Service[];

    @Input()
    set data(value: Service[]) {

        this._data = value;

        if (this.dataSource) {
            this.dataSource.data = value;
        } else {
            this.dataSource = new MatTableDataSource(value);
            this.dataSource.sortingDataAccessor = (item, property) => {
                switch (property) {
                    case 'name':
                    case 'state':
                        return item[property]?.toLowerCase() || '';
                    case 'category':
                        return this.getServiceCategory(item.name).toLowerCase();
                    default:
                        return item[property];
                }
            };
        }
    }

    get data(): Service[] {
        return this._data;
    }

    displayedColumns: string[] = [
        'name',
        'processID',
        'state',
        'category',
        'type'
    ];

    dataSource: MatTableDataSource<Service>;
    itemsPerPage = 20;

    @ViewChild(MatPaginator) paginator: MatPaginator;
    @ViewChild(MatSort) sort: MatSort;

    ngAfterViewInit(): void {
        if (this.dataSource) {
            this.dataSource.paginator = this.paginator;
            this.dataSource.sort = this.sort;
        }
    }

    applyFilter(event: Event): void {
        const filterValue = (event.target as HTMLInputElement).value;
        this.dataSource.filter = filterValue.trim().toLowerCase();
        this.dataSource.filterPredicate = (data, filter) => {
            return (
                data.name.toLowerCase().includes(filter) ||
                data.state.toLowerCase().includes(filter) ||
                data.processID.toString().includes(filter) ||
                this.getServiceCategory(data.name).toLowerCase().includes(filter)
            );
        };
        if (this.dataSource.paginator) {
            this.dataSource.paginator.firstPage();
        }
    }

    getLastValueCurrentPage(): number {
        const pageIndex = this.paginator?.pageIndex || 0;
        const pageSize = this.paginator?.pageSize || this.itemsPerPage;
        const total = this.dataSource?.filteredData?.length || 0;
        return Math.min((pageIndex + 1) * pageSize, total);
    }

    getServiceStateClass(state: string): string {
        switch (state) {
            case 'RUNNING':
                return 'bg-green-100 text-green-800';
            case 'STOPPED':
                return 'bg-red-100 text-red-800';
            case 'PAUSED':
                return 'bg-yellow-100 text-yellow-800';
            case 'PENDING':
                return 'bg-blue-100 text-blue-800';
            default:
                return 'bg-gray-100 text-gray-800';
        }
    }

    getServiceTypeClass(serviceName: string): string {
        if (!serviceName) return 'bg-gray-100 text-gray-600';

        const name = serviceName.toLowerCase();
        if (name.includes('windows') || name.includes('microsoft')) return 'bg-blue-100 text-blue-800';
        if (name.includes('google') || name.includes('chrome')) return 'bg-red-100 text-red-800';
        if (name.includes('adobe')) return 'bg-purple-100 text-purple-800';
        if (name.includes('amazon') || name.includes('aws')) return 'bg-orange-100 text-orange-800';
        if (name.includes('security') || name.includes('firewall') || name.includes('defender')) return 'bg-green-100 text-green-800';
        return 'bg-gray-100 text-gray-600';
    }

    getServiceCategory(serviceName: string): string {
        if (!serviceName) return 'Sistema';

        const name = serviceName.toLowerCase();
        if (name.includes('audio') || name.includes('som')) return 'Áudio';
        if (name.includes('network') || name.includes('rede') || name.includes('dns') || name.includes('dhcp')) return 'Rede';
        if (name.includes('security') || name.includes('firewall') || name.includes('defender') || name.includes('segurança')) return 'Segurança';
        if (name.includes('update') || name.includes('atualização')) return 'Atualização';
        if (name.includes('print') || name.includes('impressora') || name.includes('spooler')) return 'Impressão';
        if (name.includes('bluetooth')) return 'Bluetooth';
        if (name.includes('certificate') || name.includes('certificado')) return 'Certificados';
        if (name.includes('backup') || name.includes('restore')) return 'Backup';
        if (name.includes('log') || name.includes('event')) return 'Logs';
        if (name.includes('task') || name.includes('tarefa') || name.includes('scheduler')) return 'Tarefas';
        if (name.includes('google') || name.includes('chrome')) return 'Google';
        if (name.includes('adobe')) return 'Adobe';
        if (name.includes('amazon') || name.includes('aws')) return 'AWS';
        if (name.includes('microsoft') || name.includes('windows')) return 'Microsoft';
        return 'Sistema';
    }
}

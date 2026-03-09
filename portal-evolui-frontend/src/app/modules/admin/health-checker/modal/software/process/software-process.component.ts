import {AfterViewInit, Component, Input, ViewChild} from '@angular/core';
import {MatTableDataSource} from '@angular/material/table';
import {MatPaginator} from '@angular/material/paginator';
import {MatSort} from '@angular/material/sort';
import {Process} from '../../../../../../shared/models/health-checker.model';

@Component({
    selector: 'software-process',
    templateUrl: './software-process.component.html',
    styleUrls: ['./software-process.component.scss'],

    standalone: false
})
export class SoftwareProcessComponent implements AfterViewInit {

    private _data: Process[];

    @Input()
    set data(value: Process[]) {

        this._data = value;

        if (this.dataSource) {
            this.dataSource.data = value;
        } else {
            this.dataSource = new MatTableDataSource(value);
            this.dataSource.sortingDataAccessor = (item, property) => {
                switch (property) {
                    case 'user': return item.user?.toLowerCase() || '';
                    case 'commandLine': return item.commandLine?.toLowerCase() || '';
                    case 'name': return item.name?.toLowerCase() || '';
                    default: return item[property];
                }
            };
        }
    }

    get data(): Process[] {
        return this._data;
    }

    displayedColumns: string[] = [
        'name',
        'processID',
        'state',
        'cpuUsagePercent',
        'memoryUsagePercent',
        'threadCount',
        'priority',
        'user',
        'commandLine'
    ];

    dataSource: MatTableDataSource<Process>;

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

    getUsuario(process: Process): string {
        return process.user === 'unknown' ? '' : process.user;
    }

    getShortCommandLine(commandLine: string): string {
        if (!commandLine) return 'N/A';
        return commandLine.length <= 50 ? commandLine : commandLine.substring(0, 47) + '...';
    }

    getProcessStateClass(state: string): string {
        switch (state) {
            case 'RUNNING':
                return 'bg-green-100 text-green-800';
            case 'SLEEPING':
                return 'bg-blue-100 text-blue-800';
            case 'WAITING':
                return 'bg-yellow-100 text-yellow-800';
            case 'ZOMBIE':
                return 'bg-red-100 text-red-800';
            case 'STOPPED':
                return 'bg-gray-100 text-gray-800';
            default:
                return 'bg-gray-100 text-gray-800';
        }
    }

    getCpuUsageClass(cpuPercent: number): string {
        if (cpuPercent >= 80) return 'text-red-600 font-bold';
        if (cpuPercent >= 50) return 'text-yellow-600 font-semibold';
        if (cpuPercent >= 20) return 'text-blue-600';
        return 'text-gray-600';
    }

    getMemoryUsageClass(memoryPercent: number): string {
        if (memoryPercent >= 80) return 'text-red-600 font-bold';
        if (memoryPercent >= 50) return 'text-yellow-600 font-semibold';
        if (memoryPercent >= 20) return 'text-blue-600';
        return 'text-gray-600';
    }

    getPriorityClass(priority: number): string {
        if (priority >= 15) return 'bg-red-100 text-red-800';
        if (priority >= 10) return 'bg-yellow-100 text-yellow-800';
        if (priority >= 5) return 'bg-blue-100 text-blue-800';
        return 'bg-green-100 text-green-800';
    }
}

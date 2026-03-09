import {AfterViewInit, Component, Input, ViewChild} from '@angular/core';
import {MatTableDataSource} from '@angular/material/table';
import {MatPaginator} from '@angular/material/paginator';
import {MatSort} from '@angular/material/sort';
import {Session} from '../../../../../../shared/models/health-checker.model';

@Component({
    selector: 'software-session',
    templateUrl: './software-session.component.html',
    styleUrls: ['./software-session.component.scss'],

    standalone: false
})
export class SoftwareSessionComponent implements AfterViewInit {

    private _data: Session[];

    @Input()
    set data(value: Session[]) {

        this._data = value;

        if (this.dataSource) {
            this.dataSource.data = value;
        } else {
            this.dataSource = new MatTableDataSource(value);
            this.dataSource.sortingDataAccessor = (item, property) => {
                switch (property) {
                    case 'userName':
                    case 'terminalDevice':
                    case 'host':
                        return item[property]?.toLowerCase() || '';
                    case 'loginTime':
                        return item.loginTime;
                    default:
                        return (item as any)[property];
                }
            };
        }
    }

    get data(): Session[] {
        return this._data;
    }

    dataSource: MatTableDataSource<Session>;
    displayedColumns: string[] = [
        'userName',
        'terminalDevice',
        'host',
        'loginTime',
        'duration',
        'status'
    ];

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
        const filterValue = (event.target as HTMLInputElement).value.toLowerCase().trim();

        this.dataSource.filterPredicate = (session: Session, filter: string) =>
            session.userName.toLowerCase().includes(filter) ||
            session.terminalDevice.toLowerCase().includes(filter) ||
            session.host.toLowerCase().includes(filter) ||
            this.getSessionStatus(session.loginTime).toLowerCase().includes(filter);

        this.dataSource.filter = filterValue;
        if (this.paginator) this.paginator.firstPage();
    }

    getLastValueCurrentPage(): number {
        const pageIndex = this.paginator?.pageIndex || 0;
        const pageSize = this.paginator?.pageSize || this.itemsPerPage;
        const total = this.dataSource?.filteredData?.length || 0;
        return Math.min((pageIndex + 1) * pageSize, total);
    }

    trackBySession(index: number, session: Session): string {
        return `${session.userName}-${session.terminalDevice}-${session.loginTime}`;
    }

    formatLoginTime(timestamp: number): string {
        if (timestamp === 0) return 'Sistema';
        return new Date(timestamp).toLocaleString();
    }

    getSessionDuration(timestamp: number): string {
        if (timestamp === 0) return 'N/A';
        const now = Date.now();
        const duration = now - timestamp;
        const seconds = Math.floor(duration / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);
        const days = Math.floor(hours / 24);

        if (days > 0) return `${days}d ${hours % 24}h`;
        if (hours > 0) return `${hours}h ${minutes % 60}m`;
        if (minutes > 0) return `${minutes}m`;
        return `${seconds}s`;
    }

    getUserTypeClass(userName: string): string {
        switch (userName) {
            case 'SYSTEM': return 'bg-red-100 text-red-800';
            case 'LOCAL SERVICE':
            case 'NETWORK SERVICE': return 'bg-blue-100 text-blue-800';
            default: return 'bg-green-100 text-green-800';
        }
    }

    getSessionStatus(loginTime: number): string {
        return loginTime === 0 ? 'Sistema' : 'Ativa';
    }

    getSessionStatusClass(loginTime: number): string {
        return loginTime === 0 ? 'bg-gray-100 text-gray-800' : 'bg-green-100 text-green-800';
    }

    getTotalSessions(): number {
        return this.dataSource?.data?.length || 0;
    }

    getActiveSessions(): number {
        return this.dataSource?.data?.filter(s => s.loginTime > 0)?.length || 0;
    }

    getSystemSessions(): number {
        return this.dataSource?.data?.filter(s => s.loginTime === 0)?.length || 0;
    }

    getUniqueUsers(): number {
        const set = new Set(this.dataSource?.data?.map(s => s.userName));
        return set.size;
    }
}

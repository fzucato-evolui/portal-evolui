import {AfterViewInit, Component, Input, ViewChild} from '@angular/core';
import {MatTableDataSource} from '@angular/material/table';
import {MatPaginator} from '@angular/material/paginator';
import {MatSort} from '@angular/material/sort';
import {
  Connection,
  InternetProtocolStats,
  Tcpv4Stats,
  Tcpv6Stats,
  Udpv4Stats,
  Udpv6Stats
} from '../../../../../../shared/models/health-checker.model';

@Component({
    selector: 'software-protocol-stats',
    templateUrl: './software-protocol-stats.component.html',
    styleUrls: ['./software-protocol-stats.component.scss'],

    standalone: false
})
export class SoftwareProtocolStatsComponent implements AfterViewInit {

    private _data: InternetProtocolStats;

    @Input()
    set data(value: InternetProtocolStats) {
        this._data = value;
        if (value?.connections) {
            if (this.dataSource) {
                this.dataSource.data = value.connections;
            } else {
                this.dataSource = new MatTableDataSource(value.connections);
                this.dataSource.sortingDataAccessor = (item, property) => {
                    switch (property) {
                        case 'type':
                        case 'state':
                            return item[property]?.toLowerCase() || '';
                        case 'owningProcessId':
                            return item.owningProcessId;
                        case 'localPort':
                            return item.localPort;
                        case 'foreignPort':
                            return item.foreignPort;
                        default:
                            return (item as any)[property];
                    }
                };
            }
        }
    }

    get data(): InternetProtocolStats {
        return this._data;
    }

    dataSource: MatTableDataSource<Connection>;
    displayedColumns: string[] = [
        'type',
        'owningProcessId',
        'localPort',
        'foreignPort',
        'localAddress',
        'foreignAddress',
        'state'
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
        const filterValue = (event.target as HTMLInputElement).value.trim().toLowerCase();
        this.dataSource.filterPredicate = (conn: Connection, filter: string) =>
            conn.type.toLowerCase().includes(filter) ||
            conn.state.toLowerCase().includes(filter) ||
            conn.localPort.toString().includes(filter) ||
            conn.foreignPort.toString().includes(filter) ||
            conn.owningProcessId.toString().includes(filter) ||
            this.decodeBase64Address(conn.localAddress).includes(filter) ||
            this.decodeBase64Address(conn.foreignAddress).includes(filter) ||
            this.getPortTypeDescription(conn.localPort).toLowerCase().includes(filter);

        this.dataSource.filter = filterValue;
        if (this.paginator) this.paginator.firstPage();
    }

    getFirstRowOnPage(): number {
        const pageIndex = this.paginator?.pageIndex ?? 0;
        const pageSize = this.paginator?.pageSize ?? this.itemsPerPage;
        const total = this.dataSource?.filteredData?.length ?? 0;
        if (total === 0) {
            return 0;
        }
        return pageIndex * pageSize + 1;
    }

    getLastValueCurrentPage(): number {
        const pageIndex = this.paginator?.pageIndex ?? 0;
        const pageSize = this.paginator?.pageSize ?? this.itemsPerPage;
        const total = this.dataSource?.filteredData?.length ?? 0;
        return Math.min((pageIndex + 1) * pageSize, total);
    }

    trackByConnection(index: number, connection: Connection): string {
        return `${connection.type}-${connection.localPort}-${connection.foreignPort}-${connection.owningProcessId}`;
    }

    decodeBase64Address(base64Address: string): string {
        try {
            if (!base64Address) return 'N/A';
            const bytes = atob(base64Address);
            const ipBytes = Array.from(bytes).map(c => c.charCodeAt(0));

            if (ipBytes.length === 4) return ipBytes.join('.');
            if (ipBytes.length === 16) {
                return Array.from({length: 8}, (_, i) =>
                    ((ipBytes[2 * i] << 8) | ipBytes[2 * i + 1]).toString(16).padStart(4, '0')
                ).join(':');
            }
            return 'Unknown';
        } catch {
            return 'Invalid';
        }
    }

    getConnectionStateClass(state: string): string {
        switch (state) {
            case 'ESTABLISHED':
                return 'bg-green-100 text-green-800';
            case 'LISTEN':
                return 'bg-blue-100 text-blue-800';
            case 'TIME_WAIT':
                return 'bg-yellow-100 text-yellow-800';
            case 'FIN_WAIT_1':
            case 'FIN_WAIT_2':
                return 'bg-orange-100 text-orange-800';
            case 'SYN_SENT':
            case 'SYN_RECEIVED':
            case 'SYN_RECV':
                return 'bg-purple-100 text-purple-800';
            case 'CLOSE_WAIT':
            case 'CLOSING':
            case 'LAST_ACK':
                return 'bg-red-100 text-red-800';
            default:
                return 'bg-gray-100 text-gray-800';
        }
    }

    getConnectionTypeClass(type: string): string {
        switch (type) {
            case 'tcp4':
                return 'bg-blue-100 text-blue-800';
            case 'tcp6':
                return 'bg-indigo-100 text-indigo-800';
            case 'udp4':
                return 'bg-green-100 text-green-800';
            case 'udp6':
                return 'bg-orange-100 text-orange-800';
            default:
                return 'bg-gray-100 text-gray-800';
        }
    }

    getPortTypeDescription(port: number): string {
        const known = {
            21: 'FTP', 22: 'SSH', 23: 'Telnet', 25: 'SMTP', 53: 'DNS',
            80: 'HTTP', 110: 'POP3', 443: 'HTTPS', 445: 'SMB',
            993: 'IMAPS', 995: 'POP3S', 3389: 'RDP', 4200: 'Angular Dev',
            5228: 'Google Play', 8080: 'HTTP Alt', 8089: 'HTTP Alt',
            8300: 'Consul', 8500: 'Consul UI'
        };
        return known[port] || (port < 1024 ? 'System' : 'User');
    }

    getTotalConnections(): number {
        return this._data?.connections?.length || 0;
    }

    getEstablishedConnections(): number {
        return this._data?.connections?.filter(c => c.state === 'ESTABLISHED')?.length || 0;
    }

    getListeningConnections(): number {
        return this._data?.connections?.filter(c => c.state === 'LISTEN')?.length || 0;
    }

    getActiveProcesses(): number {
        const set = new Set(this._data?.connections?.map(c => c.owningProcessId).filter(pid => pid > 0));
        return set.size;
    }

    formatNumber(num: number): string {
        if (num >= 1_000_000) return (num / 1_000_000).toFixed(1) + 'M';
        if (num >= 1_000) return (num / 1_000).toFixed(1) + 'K';
        return num.toString();
    }

    getUdpv4Stats(): Udpv4Stats | null {
        return this._data?.udpv4Stats || null;
    }

    getUdpv6Stats(): Udpv6Stats | null {
        return this._data?.udpv6Stats || null;
    }

    getTcpv4Stats(): Tcpv4Stats | null {
        return this._data?.tcpv4Stats || null;
    }

    getTcpv6Stats(): Tcpv6Stats | null {
        return this._data?.tcpv6Stats || null;
    }
}

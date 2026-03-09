import {Component, Input} from '@angular/core';
import {NetworkIf} from '../../../../../../shared/models/health-checker.model';

@Component({
    selector: 'hardware-network',
    templateUrl: './hardware-network.component.html',
    styleUrls: ['./hardware-network.component.scss'],

    standalone: false
})
export class HardwareNetworkComponent {
    @Input() data: NetworkIf[] | null = null;

    isValidValue(value: any): boolean {
        return value !== null && value !== undefined && value !== '' && value !== 'unknown';
    }

    getDisplayValue(value: any): string {
        return this.isValidValue(value) ? value : 'N/A';
    }

    formatSpeed(speed: number): string {
        if (!this.isValidValue(speed) || speed === 0) {
            return 'N/A';
        }

        if (speed >= 1000000000) {
            return `${(speed / 1000000000).toFixed(1)} Gbps`;
        } else if (speed >= 1000000) {
            return `${(speed / 1000000).toFixed(1)} Mbps`;
        } else if (speed >= 1000) {
            return `${(speed / 1000).toFixed(1)} Kbps`;
        } else {
            return `${speed} bps`;
        }
    }

    formatMacAddress(mac: string): string {
        if (!this.isValidValue(mac)) {
            return 'N/A';
        }

        return mac.toUpperCase();
    }

    getStatusClass(status: string): string {
        if (!this.isValidValue(status)) {
            return 'status-unknown';
        }

        switch (status.toUpperCase()) {
            case 'UP':
                return 'status-up';
            case 'DOWN':
                return 'status-down';
            default:
                return 'status-unknown';
        }
    }

    getStatusText(status: string): string {
        if (!this.isValidValue(status)) {
            return 'Desconhecido';
        }

        switch (status.toUpperCase()) {
            case 'UP':
                return 'Ativo';
            case 'DOWN':
                return 'Inativo';
            default:
                return 'Desconhecido';
        }
    }

    getInterfaceTypeIcon(ifType: number): string {
        switch (ifType) {
            case 6:
                return 'M3 3h18a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z\n' +
                    '\n' +
                    '    M6 8h12v6h-3v3h-6v-3H6V8z\n' +
                    '\n' +
                    '    M8 8v2\n' +
                    '    M10 8v2\n' +
                    '    M12 8v2\n' +
                    '    M14 8v2\n' +
                    '    M16 8v2\n' +
                    '\n' +
                    '    M6 18h0.5\n' +
                    '    M17.5 18h0.5';
            case 53:
                return 'M8.111 16.404a5.5 5.5 0 017.778 0M12 20h.01m-7.08-7.071c3.904-3.905 10.236-3.905 14.141 0M1.394 9.393c5.857-5.857 15.355-5.857 21.213 0';
            default:
                return 'M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z';
        }
    }

    getInterfaceTypeClass(ifType: number): string {
        switch (ifType) {
            case 6:
                return 'interface-ethernet';
            case 53:
                return 'interface-wireless';
            default:
                return 'interface-unknown';
        }
    }

    getInterfaceTypeName(ifType: number): string {
        switch (ifType) {
            case 6:
                return 'Ethernet';
            case 53:
                return 'Wireless';
            default:
                return 'Desconhecido';
        }
    }

    hasValidData(): boolean {
        return this.data !== null && this.data !== undefined && Array.isArray(this.data) && this.data.length > 0;
    }

    hasIpv4Addresses(networkIf: NetworkIf): boolean {
        return networkIf.ipv4addr !== null &&
            networkIf.ipv4addr !== undefined &&
            Array.isArray(networkIf.ipv4addr) &&
            networkIf.ipv4addr.length > 0;
    }

    hasIpv6Addresses(networkIf: NetworkIf): boolean {
        return networkIf.ipv6addr !== null &&
            networkIf.ipv6addr !== undefined &&
            Array.isArray(networkIf.ipv6addr) &&
            networkIf.ipv6addr.length > 0;
    }

    getActiveInterfacesCount(): number {
        if (!this.hasValidData()) {
            return 0;
        }

        return this.data!.filter(iface => iface.ifOperStatus === 'UP').length;
    }

    getTotalSpeed(): number {
        if (!this.hasValidData()) {
            return 0;
        }

        return this.data!
            .filter(iface => iface.ifOperStatus === 'UP')
            .reduce((total, iface) => total + (iface.speed || 0), 0);
    }

    getIpv4WithMask(networkIf: NetworkIf, index: number): string {
        if (!this.hasIpv4Addresses(networkIf) || !networkIf.subnetMasks || index >= networkIf.subnetMasks.length) {
            return this.getDisplayValue(networkIf.ipv4addr?.[index]);
        }

        return `${networkIf.ipv4addr[index]}/${networkIf.subnetMasks[index]}`;
    }

    getIpv6WithPrefix(networkIf: NetworkIf, index: number): string {
        if (!this.hasIpv6Addresses(networkIf) || !networkIf.prefixLengths || index >= networkIf.prefixLengths.length) {
            return this.getDisplayValue(networkIf.ipv6addr?.[index]);
        }

        return `${networkIf.ipv6addr[index]}/${networkIf.prefixLengths[index]}`;
    }

    formatDate(timestamp: number): string {
        if (!this.isValidValue(timestamp)) {
            return 'N/A';
        }

        try {
            const date = new Date(timestamp);
            return date.toLocaleString('pt-BR');
        } catch {
            return 'N/A';
        }
    }

    getConnectorStatus(connectorPresent: boolean): string {
        return connectorPresent ? 'Conectado' : 'Desconectado';
    }

    getConnectorClass(connectorPresent: boolean): string {
        return connectorPresent ? 'connector-present' : 'connector-absent';
    }
}

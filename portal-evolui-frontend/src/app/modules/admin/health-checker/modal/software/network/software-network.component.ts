import {Component, Input} from '@angular/core';
import {NetworkParams} from "../../../../../../shared/models/health-checker.model";

@Component({
    selector: 'software-network',
    templateUrl: './software-network.component.html',
    styleUrls: ['./software-network.component.scss'],

    standalone: false
})
export class SoftwareNetworkComponent {

    @Input() data: NetworkParams;

    hasIPv6Gateway(): boolean {
        return this.data && this.data.ipv6DefaultGateway && this.data.ipv6DefaultGateway.trim() !== '';
    }

    hasDnsServers(): boolean {
        return this.data && this.data.dnsServers && this.data.dnsServers.length > 0;
    }

    getDnsServerCount(): number {
        return this.hasDnsServers() ? this.data.dnsServers.length : 0;
    }

    getShortHostname(): string {
        if (!this.data || !this.data.hostName) return 'N/A';
        return this.data.hostName.split('.')[0];
    }

    getFullDomain(): string {
        if (!this.data || !this.data.domainName) return 'N/A';
        return this.data.domainName;
    }

    isPrivateIP(ip: string): boolean {
        if (!ip) return false;

        const privateRanges = [
            /^10\./,
            /^172\.(1[6-9]|2[0-9]|3[0-1])\./,
            /^192\.168\./,
            /^127\./
        ];

        return privateRanges.some(range => range.test(ip));
    }

    getIPTypeClass(ip: string): string {
        if (this.isPrivateIP(ip)) {
            return 'bg-blue-100 text-blue-800';
        }
        return 'bg-green-100 text-green-800';
    }

    getIPTypeLabel(ip: string): string {
        if (this.isPrivateIP(ip)) {
            return 'Privado';
        }
        return 'Público';
    }

    getDnsServerType(dnsServer: string): string {
        if (!dnsServer) return 'Desconhecido';

        if (dnsServer === '8.8.8.8' || dnsServer === '8.8.4.4') return 'Google DNS';
        if (dnsServer === '1.1.1.1' || dnsServer === '1.0.0.1') return 'Cloudflare DNS';
        if (dnsServer === '208.67.222.222' || dnsServer === '208.67.220.220') return 'OpenDNS';
        if (this.isPrivateIP(dnsServer)) return 'DNS Local';
        return 'DNS Externo';
    }

    getDnsServerTypeClass(dnsServer: string): string {
        const type = this.getDnsServerType(dnsServer);

        if (type === 'Google DNS') return 'bg-red-100 text-red-800';
        if (type === 'Cloudflare DNS') return 'bg-orange-100 text-orange-800';
        if (type === 'OpenDNS') return 'bg-purple-100 text-purple-800';
        if (type === 'DNS Local') return 'bg-blue-100 text-blue-800';
        return 'bg-gray-100 text-gray-800';
    }
}


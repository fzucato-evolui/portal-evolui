import {Component, Input} from '@angular/core';
import {VersionInfo} from '../../../../../../shared/models/health-checker.model';

@Component({
    selector: 'software-version',
    templateUrl: './software-version.component.html',
    styleUrls: ['./software-version.component.scss'],

    standalone: false
})
export class SoftwareVersionComponent {
    @Input() data: VersionInfo;

    getBuildInfo(): string {
        if (!this.data || !this.data.buildNumber) return 'N/A';
        return `Build ${this.data.buildNumber}`;
    }

    hasVersionString(): boolean {
        return this.data && this.data.versionStr !== null && this.data.versionStr !== undefined && this.data.versionStr.trim() !== '';
    }

    getVersionTypeClass(): string {
        if (!this.data || !this.data.version) return 'bg-gray-100 text-gray-800';

        const version = this.data.version.toLowerCase();
        if (version.includes('server')) return 'bg-blue-100 text-blue-800';
        if (version.includes('enterprise')) return 'bg-purple-100 text-purple-800';
        if (version.includes('professional') || version.includes('pro')) return 'bg-green-100 text-green-800';
        if (version.includes('home')) return 'bg-orange-100 text-orange-800';
        return 'bg-gray-100 text-gray-800';
    }

    getCodeNameClass(): string {
        if (!this.data || !this.data.codeName) return 'bg-gray-100 text-gray-600';

        const codeName = this.data.codeName.toLowerCase();
        if (codeName.includes('datacenter')) return 'bg-indigo-100 text-indigo-800';
        if (codeName.includes('standard')) return 'bg-blue-100 text-blue-800';
        if (codeName.includes('enterprise')) return 'bg-purple-100 text-purple-800';
        if (codeName.includes('professional')) return 'bg-green-100 text-green-800';
        return 'bg-gray-100 text-gray-600';
    }
}
import {Component, Input} from '@angular/core';
import {OperatingSystem} from '../../../../../../shared/models/health-checker.model';

@Component({
    selector: 'software-info',
    templateUrl: './software-info.component.html',
    styleUrls: ['./software-info.component.scss'],

    standalone: false
})
export class SoftwareInfoComponent {

    @Input() data: OperatingSystem;

    formatUptime(seconds: number): string {
        const days = Math.floor(seconds / 86400);
        const hours = Math.floor((seconds % 86400) / 3600);
        const minutes = Math.floor((seconds % 3600) / 60);
        const secs = seconds % 60;

        let result = '';
        if (days > 0) result += `${days}d `;
        if (hours > 0) result += `${hours}h `;
        if (minutes > 0) result += `${minutes}m `;
        result += `${secs}s`;

        return result;
    }

    formatBytes(bytes: number): string {
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    formatDate(timestamp: number): string {
        return new Date(timestamp * 1000).toLocaleString();
    }

    getElevatedStatusClass(elevated: boolean): string {
        return elevated ? 'bg-red-100 text-yellow-800' : 'bg-green-100 text-green-800';
    }
}

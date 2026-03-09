import {Component, ElementRef, Input, OnInit, ViewChild} from '@angular/core';
import {
  CICDClassReportModel,
  CICDMethodTestReportModel,
  CICDProjectReportModel,
  CICDReportModel
} from 'app/shared/models/cicd.model';

@Component({
    selector: 'build-report',
    templateUrl: './build-report.component.html',
    styleUrls: ['./build-report.component.scss'],

    standalone: false
})
export class BuildReportComponent implements OnInit {
    @Input() modelMap: { [key: string]: CICDReportModel } = {};

    data: { projects: CICDProjectReportModel[] };

    selectedSectionKey: string;
    selectedSection: CICDReportModel | undefined;

    activeProject: CICDProjectReportModel | null = null;
    activeClass: CICDClassReportModel | null = null;
    activeTest: CICDMethodTestReportModel | null = null;

    activeTab: 'overview' | 'details' | 'tests' = 'overview';

    showOnlyFailedTests = false;

    totalProjects = 0;
    successfulProjects = 0;
    failedProjects = 0;
    totalTests = 0;
    successfulTests = 0;
    failedTests = 0;
    skippedTests = 0;
    totalBuildTime = 0;
    totalTestTime = 0;
    successTestTime = 0;
    failureTestTime = 0;

    groupedClasses: { [key: string]: CICDClassReportModel[] } = {};

    @ViewChild('codigo') codigoRef!: ElementRef;

    textoBotaoCopiarStacktrace = 'Copiar';

    constructor() { }

    ngOnInit(): void {
        const keys = Object.keys(this.modelMap);
        this.selectedSectionKey = keys.length > 0 ? keys[0] : '';
        this.selectedSection = this.modelMap[this.selectedSectionKey];

        if (this.selectedSection && this.selectedSection.projects) {
            this.calcularEstatisticas();
        }
    }

    onTabChange(key: string): void {
        this.selectedSectionKey = key;
        this.selectedSection = this.modelMap[key];
        this.activeProject = null;
        this.activeClass = null;
        this.activeTest = null;
        this.activeTab = 'overview';

        if (this.selectedSection && this.selectedSection.projects) {
            this.calcularEstatisticas();
        }
    }

    calcularEstatisticas(): void {
        const projects = this.selectedSection.projects;
        this.totalProjects = projects.length;

        this.successfulProjects = projects.filter(p => p.buildStatus === 'SUCCESS').length;
        this.failedProjects = this.totalProjects - this.successfulProjects;
        this.totalBuildTime = projects.reduce((sum, p) => sum + p.buildTotalTime, 0);

        this.totalTests = 0;
        this.successfulTests = 0;
        this.failedTests = 0;
        this.skippedTests = 0;
        this.totalTestTime = 0;
        this.successTestTime = 0;
        this.failureTestTime = 0;

        projects.forEach(project => {
            if (project.testSumary) {
                Object.keys(project.testSumary).forEach(status => {
                    const summary = project.testSumary[status];
                    if (summary) {
                        if (status === 'SUCCESS') {
                            this.successfulTests += summary.count;
                            this.successTestTime += summary.totalTime;
                        } else if (status === 'FAILURE') {
                            this.failedTests += summary.count;
                            this.failureTestTime += summary.totalTime;
                        } else if (status === 'SKIPPED') {
                            this.skippedTests += summary.count;
                        }
                        this.totalTests += summary.count;
                        this.totalTestTime += summary.totalTime;
                    }
                });
            }
        });
    }

    selecionarPacote(project: CICDProjectReportModel): void {
        this.activeProject = project;
        this.activeClass = null;
        this.activeTest = null;
        this.activeTab = 'details';
        this.showOnlyFailedTests = false;

        this.groupedClasses = {};
        if (project.classes && project.classes.length > 0) {
            project.classes.forEach(classInfo => {
                if (!this.groupedClasses[classInfo.name]) {
                    this.groupedClasses[classInfo.name] = [];
                }
                this.groupedClasses[classInfo.name].push(classInfo);
            });
        }
    }

    selecionarClasse(classInfos: CICDClassReportModel[]): void {
        this.activeClass = classInfos[0];
        this.activeTest = null;
        this.activeTab = 'tests';
    }

    selecionarTeste(test: CICDMethodTestReportModel): void {
        this.activeTest = test;
    }

    obterSumarioTestesAgrupado(classInfos: CICDClassReportModel[]): { [key: string]: { count: number, totalTime: number } } {
        const summary: { [key: string]: { count: number, totalTime: number } } = {};

        classInfos.forEach(classInfo => {
            if (classInfo.testSumary) {
                Object.keys(classInfo.testSumary).forEach(status => {
                    if (!summary[status]) {
                        summary[status] = { count: 0, totalTime: 0 };
                    }

                    const testSummary = classInfo.testSumary[status];
                    if (testSummary) {
                        summary[status].count += testSummary.count;
                        summary[status].totalTime += testSummary.totalTime;
                    }
                });
            }
        });

        return summary;
    }

    existemTestes(project: CICDProjectReportModel): boolean {
        return project.classes && project.classes.length > 0 &&
            project.classes.some(c => c.tests && c.tests.length > 0);
    }

    formatarTempoEmSegundos(timeInSeconds: number): string {
        if (!timeInSeconds || timeInSeconds < 0.001) {
            return '0.001s';
        } else if (timeInSeconds < 1) {
            return `${timeInSeconds.toFixed(3)}s`;
        } else if (timeInSeconds < 60) {
            return `${timeInSeconds.toFixed(2)}s`;
        } else {
            const minutes = Math.floor(timeInSeconds / 60);
            const seconds = Math.round(timeInSeconds % 60);
            return `${minutes}m ${seconds}s`;
        }
    }

    retornarBordaVermelha(status: string): string {
        return status === 'FAILURE' ? 'border-danger' : '';
    }

    obterPorcentagemTestes(): number {
        return this.totalTests > 0 ? (this.successfulTests / this.totalTests) * 100 : 0;
    }

    retornarClassePorcentagem(percentage: number): string {
        return percentage === 100 ? 'bg-success' : percentage >= 30 ? 'bg-warning' : 'bg-danger';
    }

    obterQuantidadeTestesPorSituacao(project: CICDProjectReportModel, status: string): number {
        return project.testSumary && project.testSumary[status]
            ? project.testSumary[status]!.count
            : 0;
    }

    retornarCaminhoSimplificado(path: string): string {
        const parts = path.split('/');
        if (parts.length <= 3) {
            return path;
        }
        return `.../${parts.slice(parts.length - 3).join('/')}`;
    }

    getObjectKeys(obj: any): string[] {
        return obj ? Object.keys(obj) : [];
    }

    obterClasses(): string[] {
        if (!this.showOnlyFailedTests) {
            return this.getObjectKeys(this.groupedClasses);
        }

        return this.getObjectKeys(this.groupedClasses).filter(className => {
            const summary = this.obterSumarioTestesAgrupado(this.groupedClasses[className]);
            return (summary['FAILURE'] && summary['FAILURE'].count > 0);
        });
    }

    obterTestesDaClasse(classInfos: CICDClassReportModel[]): CICDMethodTestReportModel[] {
        const allTests: CICDMethodTestReportModel[] = [];

        if (!this.showOnlyFailedTests) {
            classInfos.forEach(classInfo => {
                if (classInfo.tests && classInfo.tests.length > 0) {
                    allTests.push(...classInfo.tests);
                }
            });
            return allTests;
        }

        classInfos.forEach(classInfo => {
            if (classInfo.tests && classInfo.tests.length > 0) {
                const failedTests = classInfo.tests.filter(test => test.status === 'FAILURE');
                allTests.push(...failedTests);
            }
        });
        return allTests;
    }

    traduzirSituacao(status: string): string {
        switch (status) {
            case 'SUCCESS':
                return 'SUCESSO';
            case 'FAILURE':
                return 'FALHA';
            default:
                return status;
        }
    }

    copiarStacktrace(): void {
        const texto = this.codigoRef.nativeElement.innerText;

        window.navigator['clipboard'].writeText(texto).then(() => {
            this.textoBotaoCopiarStacktrace = 'Copiado';
            setTimeout(() => {
                this.textoBotaoCopiarStacktrace = 'Copiar';
            }, 2000);
        }).catch(() => {
            this.textoBotaoCopiarStacktrace = 'Erro';
            setTimeout(() => {
                this.textoBotaoCopiarStacktrace = 'Copiar';
            }, 2000);
        });
    }
}



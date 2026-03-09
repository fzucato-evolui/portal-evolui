import {Component, OnDestroy, OnInit, ViewChild, ViewEncapsulation} from '@angular/core';
import {ClassyLayoutComponent} from "../../../layout/layouts/classy/classy.component";
import {takeUntil} from 'rxjs/operators';
import {HomeService} from './home.service';
import {Subject} from 'rxjs';
import {HomeGithubModel, HomeModel} from '../../../shared/models/home.model';
import {ApexOptions, ChartComponent} from 'ng-apexcharts';
import {MatRadioChange} from '@angular/material/radio';
import {UtilFunctions} from '../../../shared/util/util-functions';


@Component({
  selector     : 'home.component',
  templateUrl  : './home.component.html',
  encapsulation: ViewEncapsulation.None,
  styleUrls: ['./home.component.scss'],

  standalone: false
})
export class HomeComponent implements OnInit, OnDestroy
{
  get drawerMode(): 'over' | 'side' {
    return this._parent.mobileQuery.matches ? 'over' : 'side';
  };
  get drawerOpened(): boolean {
    return this._parent.mobileQuery.matches === false;
  };
  public model: HomeModel;

  runnerPieOptions: ApexOptions;
  @ViewChild("runnerPieChart", { static: false })
  runnerPieChart: ChartComponent;

  memberPieOptions: ApexOptions;
  @ViewChild("memberPieChart", { static: false })
  memberPieChart: ChartComponent;

  repositoryPieOptions: ApexOptions;
  @ViewChild("repositoryPieChart", { static: false })
  repositoryPieChart: ChartComponent;

  commitLineOptions: Partial<ApexOptions>;
  @ViewChild("commitLineChart", { static: false })
  commitLineChart: ChartComponent;

  commiterPieOptions: {[key: string]: ApexOptions} = {};
  geracaoVersaoPieOptions: {[key: string]: ApexOptions} = {};
  atualizacaoVersaoPieOptions: {[key: string]: ApexOptions} = {};
  ambientePieOptions: {[key: string]: ApexOptions} = {};
  branchPieOptions: {[key: string]: ApexOptions} = {};
  cicdStackedBarOptions: {[key: string]: Partial<ApexOptions>} = {};
  currentCICDChart = null;
  cicdKeys = [];
  private _onDestroy = new Subject<void>();
  isDark: boolean = false;
  constructor(
    private _parent: ClassyLayoutComponent,
    private _service: HomeService
  )
  {
    if (this._parent.user?.config?.scheme === 'dark') {
      this.isDark = true;
    }
  }

  ngOnDestroy(): void {
    this._onDestroy.next();
    this._onDestroy.complete();
  }


  ngOnInit(): void
  {
    this.model = new HomeModel();
    this.model.gitHub = new HomeGithubModel();
    this.model.cicds = [];
    this._service.model$.pipe((takeUntil(this._onDestroy)))
      .subscribe((value: HomeModel) => {
        if (value) {
          this.model = value;
          this.loadCharts();
        }

      });


  }
  loadCharts() {
    this.runnerPieOptions = {
      legend: {
        show: false,
      },
      theme: {
        mode: this.isDark ? 'dark' : 'light',
        monochrome: {
          enabled: true,
          color: "#3182CE"
        }
      },
      chart      : {
        animations: {
          speed           : 400,
          animateGradually: {
            enabled: false,
          },
        },
        fontFamily: 'inherit',
        foreColor : this.isDark ? 'rgba(255,255,255,0.87)' : '#373d3f',
        background: 'transparent',
        height    : '100%',
        width    : '100%',
        type      : 'donut',
        sparkline : {
          enabled: true,
        },
      },
      labels     : Object.keys(this.model.gitHub.runners.series),
      plotOptions: {
        pie: {
          customScale  : 0.9,
          expandOnClick: false,
          donut        : {
            size: '70%',
            labels: {
              show: true,
              total: {
                showAlways: true,
                show: true,
                label: 'Total',
                formatter: (w: any) => {
                  const series = w.config.series;
                  const total = Object.keys(this.model.gitHub.runners.series).map(x => this.model.gitHub.runners.series[x] as number).reduce(function (x, y) {
                    return x + y;
                  });
                  return total.toString();
                }
              }
            }
          },
        },
      },
      series     : Object.values(this.model.gitHub.runners.series),
      states     : {
        hover : {
          filter: {
            type: 'none',
          },
        },
        active: {
          filter: {
            type: 'none',
          },
        },
      },

    };
    this.memberPieOptions = {
      legend: {
        show: false,
      },
      theme: {
        mode: this.isDark ? 'dark' : 'light',
        monochrome: {
          enabled: true,
          color: "#805AD5"
        }
      },
      chart      : {
        animations: {
          speed           : 400,
          animateGradually: {
            enabled: false,
          },
        },
        fontFamily: 'inherit',
        foreColor : this.isDark ? 'rgba(255,255,255,0.87)' : '#373d3f',
        background: 'transparent',
        height    : '100%',
        width    : '100%',
        type      : 'donut',
        sparkline : {
          enabled: true,
        },
      },
      labels     : Object.keys(this.model.gitHub.members.series),
      plotOptions: {
        pie: {
          customScale  : 0.9,
          expandOnClick: false,
          donut        : {
            size: '70%',
            labels: {
              show: true,
              total: {
                showAlways: true,
                show: true,
                label: 'Total',
                formatter: (w: any) => {
                  try {
                    const series = w.config.series;
                    const total = Object.keys(this.model.gitHub.members.series).map(x => this.model.gitHub.members.series[x] as number).reduce(function (x, y) {
                      return x + y;
                    });
                    return total.toString();
                  }
                  catch (error) {
                    return "0";
                  }
                }
              }
            }
          },
        },
      },
      series     : Object.values(this.model.gitHub.members.series),
      states     : {
        hover : {
          filter: {
            type: 'none',
          },
        },
        active: {
          filter: {
            type: 'none',
          },
        },
      },

    };
    this.repositoryPieOptions = {
      legend: {
        show: false,
      },
      theme: {
        mode: this.isDark ? 'dark' : 'light',
        monochrome: {
          enabled: true,
          color: "#DD6B20"
        }
      },
      chart      : {
        animations: {
          speed           : 400,
          animateGradually: {
            enabled: false,
          },
        },
        fontFamily: 'inherit',
        foreColor : this.isDark ? 'rgba(255,255,255,0.87)' : '#373d3f',
        background: 'transparent',
        height    : '100%',
        width    : '100%',
        type      : 'donut',
        sparkline : {
          enabled: true,
        },
      },
      labels     : Object.keys(this.model.gitHub.repositories.series),
      plotOptions: {
        pie: {
          customScale  : 0.9,
          expandOnClick: false,
          donut        : {
            size: '70%',
            labels: {
              show: true,
              total: {
                showAlways: true,
                show: true,
                label: 'Total',
                formatter: (w: any) => {
                  try {
                    const series = w.config.series;
                    const total = Object.keys(this.model.gitHub.repositories.series).map(x => this.model.gitHub.repositories.series[x] as number).reduce(function (x, y) {
                      return x + y;
                    });
                    return total.toString();
                  }
                  catch (e) {
                    return "0";
                  }
                }
              }
            }
          },
        },
      },
      series     : Object.values(this.model.gitHub.repositories.series),
      states     : {
        hover : {
          filter: {
            type: 'none',
          },
        },
        active: {
          filter: {
            type: 'none',
          },
        },
      },

    };
    this.commitLineOptions = {
      theme: {
        mode: this.isDark ? 'dark' : 'light',
      },
      series: Object.keys(this.model.gitHub.productCommits.series).map(s =>
      {
        let serieArray = {
          name: this._parent.projects.filter(p => p.id.toString() === s)[0].identifier,
          data: this.model.gitHub.productCommits.series[s].map(c => {
            let d = new Date(c.x);
            return {x: d, y: c.y}
          })
        }
        return serieArray;
      }),
      chart: {
        id: 'realtime',
        height: '100%',
        width: '100%',
        type: 'line',
        foreColor: this.isDark ? 'rgba(255,255,255,0.87)' : '#373d3f',
        background: 'transparent',
        animations: {
          enabled: true,
          dynamicAnimation: {
            speed: 1000
          }
        },
        toolbar: {
          show: false
        },
        zoom: {
          enabled: false
        }
      },
      dataLabels: {
        enabled: false
      },
      stroke: {
        curve: 'straight'
      },
      markers: {
        size: 0
      },
      xaxis: {
        type: 'datetime',
        labels: {
          show: true,
          //datetimeUTC: false
        }
      },
      yaxis: {
        labels: {
          show: true,

        }
      },
      legend: {
        show: true
      },
    };

    this.commiterPieOptions = {};
    this.ambientePieOptions = {};
    this.atualizacaoVersaoPieOptions = {};
    this.geracaoVersaoPieOptions = {};
    this.branchPieOptions = {};
    this.cicdStackedBarOptions = {};
    let colors = ['#2E93fA', '#66DA26', '#546E7A', '#E91E63', '#FF9800'];
    let countSeries = 0;
    Object.keys(this.model.gitHub.mainCommiters).forEach(s => {

      this.commiterPieOptions[s] = {
        legend: {
          show: false,
        },
        theme: {
          mode: this.isDark ? 'dark' : 'light',
          monochrome: {
            enabled: true,
            color: colors[countSeries]
          }
        },
        chart      : {
          animations: {
            speed           : 400,
            animateGradually: {
              enabled: false,
            },
          },
          fontFamily: 'inherit',
          foreColor : this.isDark ? 'rgba(255,255,255,0.87)' : '#373d3f',
        background: 'transparent',
          height    : '100%',
          width    : '100%',
          type      : 'donut',
          sparkline : {
            enabled: true,
          },
        },
        labels     : Object.keys(this.model.gitHub.mainCommiters[s].series),
        plotOptions: {
          pie: {
            customScale  : 0.9,
            expandOnClick: false,
            donut        : {
              size: '70%',
              labels: {
                show: true,
                total: {
                  showAlways: true,
                  show: true,
                  label: 'Total',
                  formatter: (w: any) => {
                    const series = w.config.series;
                    const total = Object.keys(this.model.gitHub.mainCommiters[s].series).map(x => this.model.gitHub.mainCommiters[s].series[x] as number).reduce(function (x, y) {
                      return x + y;
                    });
                    return total.toString();
                  }
                }
              }
            },
          },
        },
        series     : Object.values(this.model.gitHub.mainCommiters[s].series),
        states     : {
          hover : {
            filter: {
              type: 'none',
            },
          },
          active: {
            filter: {
              type: 'none',
            },
          },
        },

      };
      countSeries++;
    });
    countSeries = 0;
    Object.keys(this.model.beansCountAmbiente).forEach(s => {

      this.ambientePieOptions[s] = {
        legend: {
          show: false,
        },
        theme: {
          mode: this.isDark ? 'dark' : 'light',
          monochrome: {
            enabled: true,
            color: colors[countSeries]
          }
        },
        chart      : {
          animations: {
            speed           : 400,
            animateGradually: {
              enabled: false,
            },
          },
          fontFamily: 'inherit',
          foreColor : this.isDark ? 'rgba(255,255,255,0.87)' : '#373d3f',
        background: 'transparent',
          height    : '100%',
          width    : '100%',
          type      : 'donut',
          sparkline : {
            enabled: true,
          },
        },
        labels     : Object.keys(this.model.beansCountAmbiente[s].series),
        plotOptions: {
          pie: {
            customScale  : 0.9,
            expandOnClick: false,
            donut        : {
              size: '70%',
              labels: {
                show: true,
                total: {
                  showAlways: true,
                  show: true,
                  label: 'Total',
                  formatter: (w: any) => {
                    const series = w.config.series;
                    const total = Object.keys(this.model.beansCountAmbiente[s].series).map(x => this.model.beansCountAmbiente[s].series[x] as number).reduce(function (x, y) {
                      return x + y;
                    });
                    return total.toString();
                  }
                }
              }
            },
          },
        },
        series     : Object.values(this.model.beansCountAmbiente[s].series),
        states     : {
          hover : {
            filter: {
              type: 'none',
            },
          },
          active: {
            filter: {
              type: 'none',
            },
          },
        },

      };
      countSeries++;
    });
    countSeries = 0;
    Object.keys(this.model.beansCountGeracaoVersao).forEach(s => {

      this.geracaoVersaoPieOptions[s] = {
        legend: {
          show: false,
        },
        theme: {
          mode: this.isDark ? 'dark' : 'light',
          monochrome: {
            enabled: true,
            color: colors[countSeries]
          }
        },
        chart      : {
          animations: {
            speed           : 400,
            animateGradually: {
              enabled: false,
            },
          },
          fontFamily: 'inherit',
          foreColor : this.isDark ? 'rgba(255,255,255,0.87)' : '#373d3f',
        background: 'transparent',
          height    : '100%',
          width    : '100%',
          type      : 'donut',
          sparkline : {
            enabled: true,
          },
        },
        labels     : Object.keys(this.model.beansCountGeracaoVersao[s].series),
        plotOptions: {
          pie: {
            customScale  : 0.9,
            expandOnClick: false,
            donut        : {
              size: '70%',
              labels: {
                show: true,
                total: {
                  showAlways: true,
                  show: true,
                  label: 'Total',
                  formatter: (w: any) => {
                    const series = w.config.series;
                    const total = Object.keys(this.model.beansCountGeracaoVersao[s].series).map(x => this.model.beansCountGeracaoVersao[s].series[x] as number).reduce(function (x, y) {
                      return x + y;
                    });
                    return total.toString();
                  }
                }
              }
            },
          },
        },
        series     : Object.values(this.model.beansCountGeracaoVersao[s].series),
        states     : {
          hover : {
            filter: {
              type: 'none',
            },
          },
          active: {
            filter: {
              type: 'none',
            },
          },
        },

      };
      countSeries++;
    });
    countSeries = 0;
    Object.keys(this.model.beansCountAtualizazaoVersao).forEach(s => {

      this.atualizacaoVersaoPieOptions[s] = {
        legend: {
          show: false,
        },
        theme: {
          mode: this.isDark ? 'dark' : 'light',
          monochrome: {
            enabled: true,
            color: colors[countSeries]
          }
        },
        chart      : {
          animations: {
            speed           : 400,
            animateGradually: {
              enabled: false,
            },
          },
          fontFamily: 'inherit',
          foreColor : this.isDark ? 'rgba(255,255,255,0.87)' : '#373d3f',
        background: 'transparent',
          height    : '100%',
          width    : '100%',
          type      : 'donut',
          sparkline : {
            enabled: true,
          },
        },
        labels     : Object.keys(this.model.beansCountAtualizazaoVersao[s].series),
        plotOptions: {
          pie: {
            customScale  : 0.9,
            expandOnClick: false,
            donut        : {
              size: '70%',
              labels: {
                show: true,
                total: {
                  showAlways: true,
                  show: true,
                  label: 'Total',
                  formatter: (w: any) => {
                    const series = w.config.series;
                    const total = Object.keys(this.model.beansCountAtualizazaoVersao[s].series).map(x => this.model.beansCountAtualizazaoVersao[s].series[x] as number).reduce(function (x, y) {
                      return x + y;
                    });
                    return total.toString();
                  }
                }
              }
            },
          },
        },
        series     : Object.values(this.model.beansCountAtualizazaoVersao[s].series),
        states     : {
          hover : {
            filter: {
              type: 'none',
            },
          },
          active: {
            filter: {
              type: 'none',
            },
          },
        },

      };
      countSeries++;
    });
    countSeries = 0;
    Object.keys(this.model.beansCountBranch).forEach(s => {

      this.branchPieOptions[s] = {
        legend: {
          show: false,
        },
        theme: {
          mode: this.isDark ? 'dark' : 'light',
          monochrome: {
            enabled: true,
            color: colors[countSeries]
          }
        },
        chart      : {
          animations: {
            speed           : 400,
            animateGradually: {
              enabled: false,
            },
          },
          fontFamily: 'inherit',
          foreColor : this.isDark ? 'rgba(255,255,255,0.87)' : '#373d3f',
        background: 'transparent',
          height    : '100%',
          width    : '100%',
          type      : 'donut',
          sparkline : {
            enabled: true,
          },
        },
        labels     : Object.keys(this.model.beansCountBranch[s].series),
        plotOptions: {
          pie: {
            customScale  : 0.9,
            expandOnClick: false,
            donut        : {
              size: '70%',
              labels: {
                show: true,
                total: {
                  showAlways: true,
                  show: true,
                  label: 'Total',
                  formatter: (w: any) => {
                    const series = w.config.series;
                    const total = Object.keys(this.model.beansCountBranch[s].series).map(x => this.model.beansCountBranch[s].series[x] as number).reduce(function (x, y) {
                      return x + y;
                    });
                    return total.toString();
                  }
                }
              }
            },
          },
        },
        series     : Object.values(this.model.beansCountBranch[s].series),
        states     : {
          hover : {
            filter: {
              type: 'none',
            },
          },
          active: {
            filter: {
              type: 'none',
            },
          },
        },

      };
      countSeries++;
    });
    this.model.cicds.forEach(s => {
      const key = this._parent.projects.filter(x => x.id === s.produtoId)[0].identifier + '-' + s.branch;
      if (Object.keys(this.cicdStackedBarOptions).includes(key) === false) {
        this.cicdKeys.push(key);
        if (this.cicdKeys.length === 1) {
          this.currentCICDChart = key;
        }
        this.cicdStackedBarOptions[key] = {
          theme: {
            mode: this.isDark ? 'dark' : 'light',
          },
          chart: {
            id: 'realtime',
            height: '100%',
            width: '100%',
            stacked: true,
            type: "bar",
            foreColor: this.isDark ? 'rgba(255,255,255,0.87)' : '#373d3f',
            background: 'transparent',
            animations: {
              enabled: true,
                  dynamicAnimation: {
                speed: 1000
              }
            },
            toolbar: {
              show: true,
              tools: {
                zoom: true
              }
            },
            zoom: {
              enabled: true
            }
          },
          dataLabels: {
            enabled: false
          },
          stroke: {
            curve: 'straight'
          },
          markers: {
            size: 0
          },
          xaxis: {
            type: 'datetime',

            labels: {
              show: true,
              //datetimeUTC: false
            }
          },
          yaxis: {
            labels: {
              show: true,

            }
          },
          series: [],
          plotOptions: {
            bar: {
              horizontal: false
            }
          },
          legend: {
            show: true,
            position: "right",
            offsetY: 40
          }
        };
        s.chart.labels.forEach(l => {
          const value = {
            name: l,
            data: s.chart.series.map(k => {
              let xValue = new Date(k.x);
              let yValue = k.y[l] as number;
              if (UtilFunctions.isValidStringOrArray(yValue) === false) {
                yValue = 0;
              }
              return {x: xValue, y: yValue}
            })
          }
          // @ts-ignore
          this.cicdStackedBarOptions[key].series.push(value);
        });


      }


    })
  }

  getCommitGithubDays() {
    return HomeService.githubCommitDays;
  }
  getBeanDays() {
    return HomeService.beanDays;
  }
  getCICDDays() {
    return HomeService.cicdDays;
  }

  getProductFromId(key: string) {
    return this._parent.projects.filter(x => x.id.toString() === key)[0].identifier;
  }

  getTotalRunners() {
    try {
      const total = Object.keys(this.model.gitHub.runners.series).map(x => this.model.gitHub.runners.series[x] as number).reduce(function (x, y) {
        return x + y;
      });
      return total.toString();
    }
    catch (e) {
      return "0";
    }
  }

  getTotalRunnersStatus(key: string) {
    try {
      if (Object.keys(this.model.gitHub.runners.series).includes(key) === true) {
        const total = Object.keys(this.model.gitHub.runners.series).filter(x => x.includes(key)).map(x => this.model.gitHub.runners.series[x] as number).reduce(function (x, y) {
          return x + y;
        });
        return total.toString();
      }
    }
    catch (e) {

    }
    return '0';
  }

  getTotalMembers() {
    try {
      const total = Object.keys(this.model.gitHub.members.series).map(x => this.model.gitHub.members.series[x] as number).reduce(function (x, y) {
        return x + y;
      });
      return total.toString();
    }
    catch (e) {
      return "0";
    }
  }

  getTotalMembersStatus(key: string) {
    try {
      if (Object.keys(this.model.gitHub.members.series).includes(key) === true) {
        const total = Object.keys(this.model.gitHub.members.series).filter(x => x.includes(key)).map(x => this.model.gitHub.members.series[x] as number).reduce(function (x, y) {
          return x + y;
        });
        return total.toString();
      }
    }
    catch (e) {

    }
    return '0';
  }

  getTotalRepositories() {
    try {
      const total = Object.keys(this.model.gitHub.repositories.series).map(x => this.model.gitHub.repositories.series[x] as number).reduce(function (x, y) {
        return x + y;
      });
      return total.toString();
    }
    catch (e) {

    }
    return "0";
  }

  getTotalRepositoriesStatus(key: string) {
    try {
      if (Object.keys(this.model.gitHub.repositories.series).includes(key) === true) {
        const total = Object.keys(this.model.gitHub.repositories.series).filter(x => x.includes(key)).map(x => this.model.gitHub.repositories.series[x] as number).reduce(function (x, y) {
          return x + y;
        });
        return total.toString();
      }
    }
    catch (e) {

    }
    return '0';
  }

  unsorted() { }

  cicdRadioChanged(e: MatRadioChange) {
    this.currentCICDChart = e.value;
    console.log(this.currentCICDChart);
    console.log(this.cicdStackedBarOptions, this.cicdStackedBarOptions[this.currentCICDChart]);
  }


}

// 프레임 타이밍 페이지의 차트. 데이터는 서버가 페이지 안에 넣어 두므로 이 스크립트는 요청을 하지
// 않는다. Chart.js 로드에 실패해도 표에 모든 수치가 남아 있으므로 조용히 빠져나간다.
(function () {
    'use strict';

    function readJson(id) {
        var el = document.getElementById(id);
        if (!el) {
            return [];
        }
        try {
            return JSON.parse(el.textContent) || [];
        } catch (e) {
            return [];
        }
    }

    if (typeof Chart === 'undefined') {
        return;
    }

    var timings = readJson('timingData');
    var timingCanvas = document.getElementById('timingChart');
    if (timingCanvas && timings.length) {
        new Chart(timingCanvas, {
            type: 'bar',
            data: {
                labels: timings.map(function (t) { return t.name; }),
                datasets: [
                    {
                        label: 'median (ms)',
                        data: timings.map(function (t) { return t.median; }),
                        backgroundColor: 'rgba(13, 110, 253, 0.7)'
                    },
                    {
                        label: 'p95 (ms)',
                        data: timings.map(function (t) { return t.p95; }),
                        backgroundColor: 'rgba(253, 126, 20, 0.7)'
                    }
                ]
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: {
                        title: { display: true, text: 'milliseconds' },
                        beginAtZero: true
                    }
                }
            }
        });
    }

    var rate = readJson('sessionRateData');
    var rateCanvas = document.getElementById('sessionRateChart');
    if (rateCanvas && rate.length) {
        new Chart(rateCanvas, {
            type: 'bar',
            data: {
                labels: rate.map(function (p) { return p.at; }),
                datasets: [{
                    label: 'games / hour',
                    data: rate.map(function (p) { return p.perHour; }),
                    // 잘린 구간은 옅게 칠해 "값이 낮다"와 "아직 안 찼다"를 구분한다.
                    backgroundColor: rate.map(function (p) {
                        return p.partial ? 'rgba(13, 110, 253, 0.25)' : 'rgba(13, 110, 253, 0.7)';
                    }),
                    borderColor: 'rgba(13, 110, 253, 1)',
                    borderWidth: rate.map(function (p) { return p.partial ? 1 : 0; })
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    tooltip: {
                        callbacks: {
                            afterLabel: function (item) {
                                var point = rate[item.dataIndex];
                                if (!point) {
                                    return '';
                                }
                                return point.games + ' games' + (point.partial ? ' (구간이 잘림)' : '');
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        title: { display: true, text: 'games / hour' },
                        beginAtZero: true
                    }
                }
            }
        });
    }

    var series = readJson('seriesData');
    var seriesCanvas = document.getElementById('seriesChart');
    if (seriesCanvas && series.length) {
        new Chart(seriesCanvas, {
            type: 'line',
            data: {
                labels: series.map(function (p) { return p.at; }),
                datasets: [{
                    label: 'median (ms)',
                    data: series.map(function (p) { return p.median; }),
                    borderColor: 'rgba(13, 110, 253, 1)',
                    backgroundColor: 'rgba(13, 110, 253, 0.2)',
                    pointRadius: 2,
                    tension: 0.1
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    tooltip: {
                        callbacks: {
                            // 그 구간에 게임이 몇 건이었는지가 값의 신뢰도를 좌우한다.
                            afterLabel: function (item) {
                                var point = series[item.dataIndex];
                                return point ? point.games + ' games' : '';
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        title: { display: true, text: 'milliseconds' },
                        beginAtZero: true
                    }
                }
            }
        });
    }
})();

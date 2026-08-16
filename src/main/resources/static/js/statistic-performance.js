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
    var companionSeries = readJson('companionSeriesData');
    var seriesCanvas = document.getElementById('seriesChart');
    if (seriesCanvas && series.length) {
        // 두 선의 구간이 항상 같지는 않다. 한쪽에만 게임이 있는 구간은 축에 남기고 값만 비워야
        // 선이 옆 구간으로 이어 붙어 없는 데이터를 있는 것처럼 그리지 않는다.
        var labels = [];
        var seen = {};
        [series, companionSeries].forEach(function (points) {
            points.forEach(function (p) {
                if (!seen[p.at]) {
                    seen[p.at] = true;
                    labels.push(p.at);
                }
            });
        });
        labels.sort();

        function byBucket(points) {
            var map = {};
            points.forEach(function (p) { map[p.at] = p; });
            return map;
        }

        function dataset(name, points, borderColor, fillColor) {
            var map = byBucket(points);
            return {
                label: (name || 'median') + ' (ms)',
                data: labels.map(function (at) {
                    return map[at] ? map[at].median : null;
                }),
                borderColor: borderColor,
                backgroundColor: fillColor,
                pointRadius: 2,
                tension: 0.1,
                spanGaps: false,
                // 게임 수는 툴팁에서 쓴다. 구간이 비어 있으면 값도 없다.
                buckets: map
            };
        }

        var datasets = [dataset(seriesCanvas.dataset.primaryName, series,
                'rgba(13, 110, 253, 1)', 'rgba(13, 110, 253, 0.2)')];
        if (companionSeries.length) {
            datasets.push(dataset(seriesCanvas.dataset.companionName, companionSeries,
                    'rgba(253, 126, 20, 1)', 'rgba(253, 126, 20, 0.2)'));
        }

        new Chart(seriesCanvas, {
            type: 'line',
            data: {
                labels: labels,
                datasets: datasets
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    tooltip: {
                        callbacks: {
                            // 그 구간에 게임이 몇 건이었는지가 값의 신뢰도를 좌우한다.
                            afterLabel: function (item) {
                                var buckets = item.dataset.buckets || {};
                                var point = buckets[item.label];
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

package es.eriktorr
package attribution.health

import cats.effect.IO
import es.eriktorr.attribution.model.ConversionInstance.ConversionAction
import fs2.Stream
import fs2.io.file.{Files, Path}

import java.nio.file.Paths as JPaths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@SuppressWarnings(Array("org.wartremover.warts.Any"))
final class PipelineHealthDashboard(
    artifactPath: String,
):
  def generate(
      conversionAction: ConversionAction,
      results: List[VerificationResult],
  ): IO[Unit] =
    for
      filePath <- IO.blocking(
        Path.fromNioPath(JPaths.get(artifactPath)),
      )
      content = htmlContentFrom(conversionAction, results.sortBy(_.date))
      _ <- writeToFile(content, filePath).compile.drain
    yield ()

  private def htmlContentFrom(
      conversionAction: ConversionAction,
      results: List[VerificationResult],
  ) =
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    val labels = results.map(result => s"'${result.date}'").mkString(",")
    val actualData = results.map(_.successRate).mkString(",")
    val expectedData = results.map(_.minimumSuccessThreshold).mkString(",")

    def renderResultRow(result: VerificationResult) =
      val status = result.status
      val statusClass =
        if status == VerificationResult.Status.Pass then "status-pass"
        else "status-fail"
      s"""
         |          <tr>
         |            <td>${result.date}</td>
         |            <td>${result.minimumSuccessThreshold}</td>
         |            <td>${result.successRate}</td>
         |            <td class="$statusClass">${status.name}</td>
         |          </tr>"""

    s"""
       |<html>
       |<head>
       |  <meta http-equiv="refresh" content="300">
       |  <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
       |  <style>
       |    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 40px; background: #f0f2f5; }
       |    .container { max-width: 900px; margin: auto; background: white; padding: 30px; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.08); }
       |
       |    .header-row {
       |      display: flex;
       |      justify-content: space-between;
       |      align-items: center;
       |      border-bottom: 2px solid #f0f2f5;
       |      padding-bottom: 15px;
       |      margin-bottom: 20px;
       |    }
       |    .customer-id { font-size: 1.2em; color: #333; font-weight: 600; }
       |    .freshness { font-size: 0.9em; color: #666; display: flex; align-items: center; }
       |
       |    .live-indicator {
       |      display: inline-block;
       |      width: 10px; height: 10px;
       |      background-color: #28a745;
       |      border-radius: 50%;
       |      margin-right: 8px;
       |      animation: pulse 2s infinite;
       |    }
       |    @keyframes pulse {
       |      0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(40, 167, 69, 0.7); }
       |      70% { transform: scale(1); box-shadow: 0 0 0 10px rgba(40, 167, 69, 0); }
       |      100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(40, 167, 69, 0); }
       |    }
       |
       |    .chart-container { margin: 30px 0; height: 300px; }
       |    table { width: 100%; border-collapse: collapse; margin-top: 20px; }
       |    th { background: #f8f9fa; color: #555; }
       |    th, td { padding: 12px; text-align: left; border-bottom: 1px solid #eee; }
       |    .status-pass { color: #28a745; font-weight: bold; }
       |    .status-fail { color: #dc3545; font-weight: bold; }
       |  </style>
       |</head>
       |<body>
       |  <div class="container">
       |    <div class="header-row">
       |      <div class="customer-id">
       |        <span style="color: #666; font-weight: 400;">Conversion Action:</span> $conversionAction
       |      </div>
       |      <div class="freshness">
       |        <span class="live-indicator"></span>
       |        <b>Data Freshness:</b>&nbsp;$timestamp
       |      </div>
       |    </div>
       |
       |    <h1>📈 Pipeline Health Trend</h1>
       |    <div class="chart-container">
       |      <canvas id="trendChart"></canvas>
       |    </div>
       |
       |    <table>
       |      <thead>
       |        <tr><th>Date</th><th>Expected</th><th>Actual</th><th>Status</th></tr>
       |      </thead>
       |      <tbody>
       |        ${results.reverse.map(renderResultRow).mkString}
       |      </tbody>
       |    </table>
       |  </div>
       |
       |  <script>
       |    const ctx = document.getElementById('trendChart').getContext('2d');
       |    new Chart(ctx, {
       |      type: 'line',
       |      data: {
       |        labels: [$labels],
       |        datasets: [{
       |          label: 'Actual Conversions',
       |          data: [$actualData],
       |          borderColor: '#28a745',
       |          backgroundColor: 'rgba(40, 167, 69, 0.1)',
       |          fill: true,
       |          tension: 0.3
       |        }, {
       |          label: 'Expected (Baseline)',
       |          data: [$expectedData],
       |          borderColor: '#6c757d',
       |          borderDash: [5, 5],
       |          fill: false
       |        }]
       |      },
       |      options: {
       |        responsive: true,
       |        maintainAspectRatio: false,
       |        scales: { y: { beginAtZero: true } }
       |      }
       |    });
       |  </script>
       |</body>
       |</html>
       |""".stripMargin

  private def writeToFile(
      content: String,
      filePath: Path,
  ) =
    Stream
      .emit(content)
      .through(fs2.text.utf8.encode)
      .through(Files[IO].writeAll(filePath))

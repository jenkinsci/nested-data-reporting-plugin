<!-- PROJECT LOGO -->
<br />
<p align="center">
  <a href="#">
   <img src="src/main/webapp/icons/wm-logo-48x48.png" alt="Logo" width="40" height="40">
  </a>

  <h1 align="center">Data Reporting Plugin</h1>

  <p align="center">
    Jenkins plugin to report data from csv as pie- and trend-charts.
    <br />
    <a href="https://github.com/simonsymhoven/data-reporting-plugin/blob/master/README.md"><strong>Explore the docs »</strong></a>
    <br />
    <br />
    <a href="https://github.com/simonsymhoven/data-reporting-plugin/issues">Report Bug</a>
    ·
    <a href="https://github.com/simonsymhoven/data-reporting-plugin/issues">Request Feature</a>
  </p>
</p>

## Introduction

This plugin reports data from csv file as pie- and trend-charts. An example csv file looks like this:

```
Product;accurate;manually;incorrect
Aktien;9;2;3
Derivate;10;3;2
Fonds;20;7;6
Optionsscheine;15;4;6
```

At job level, a trend chart is generated showing the development 
of the asset classes included in the csv over all builds.

At the build level, the distribution is then displayed as 
a pie chart for each asset class and the detailed history of the last 
builds is displayed in a filterable manner.

In addition, the percentage distribution is shown 
in total across all classes in the summary.

## Getting started

### Pipeline Step

```
reportData csv: 'stocks.csv', label: 'Data Reporting (WM 2022)' 
```

Parameter: 

* csv: path to csv file relative to the workspace.
* label: the label for the build action (optional, default: "Data Reporting").

## Issues

TODO Decide where you're going to host your issues, the default is Jenkins JIRA, but you can also enable GitHub issues,
If you use GitHub issues there's no need for this section; else add the following line:

Report issues and enhancements in the [Jenkins issue tracker](https://issues.jenkins-ci.org/).

## Contributing

Contributions are what make the open source community such an amazing place to be learn,
inspire, and create. Any contributions you make are **greatly appreciated**.

1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)


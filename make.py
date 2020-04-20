#!/usr/bin/env python3

# NOTE: This script requires python 3.7 or higher.

import argparse
import configparser
import os
import shutil
import subprocess
import sys


def read_awscreds(profile='default', credentials_path='~/.aws/credentials'):
    """
    Reads AWS credentials from an AWS config file.

    Args:
        profile: The profile to use.
        credentials_path: The local path to the credentials file.

    Return:
        A dictionary with two string entries, `access_key_id` and
        `secret_access_key`.
    """
    config = configparser.ConfigParser()
    config.read(os.path.expanduser(credentials_path))
    creds = config[profile]
    return {
        "access_key_id": creds["aws_access_key_id"],
        "secret_access_key": creds["aws_secret_access_key"],
    }


class Docker:
    """
    All Docker-related commands.
    """

    DOCKER_IMAGE = "hchauvin/exploratory-pipeline-example"
    CONTAINER_NAME = "exploratory-pipeline-example"

    def __init__(self, dev):
        self._env = {**os.environ, "DOCKER_BUILDKIT": "1"}
        self._dev = dev

    def docker_run(self, sbt, build, volumes, extra_parameters, aws_profile):
        if sbt:
            self._dev.package()
        if build:
            self.docker_build()

        subprocess.run(["docker", "rm", "-f", Docker.CONTAINER_NAME],
                       check=False,
                       capture_output=True,
                       env=self._env)

        cwd = os.getcwd()

        args = [
            "docker",
            "run",
            "--rm",
            "--name",
            Docker.CONTAINER_NAME,
            "-v",
            f"{cwd}/notebooks:/opt/notebooks",
            "-v",
            f"{cwd}/bioinformatics/src/main/resources/io/hchauvin/bio/workflows:/opt/workflows:ro",
            "-v",
            os.path.expanduser("~/.aws") + ":/root/.aws:ro",
            "-v",
            os.path.expanduser("~/.reflow2") + ":/root/.reflow:rw",
            "-v",
            os.path.expanduser("~/.ssh") + ":/root/.ssh:ro",
            "-e", "AWS_ACCESS_KEY_ID",
            "-e", "AWS_SECRET_ACCESS_KEY",
            "-e", "LD_LIBRARY_PATH=/usr/local/lib/R/site-library/rJava/jri",
            "-e", "R_HOME=/usr/lib/R",
        ]
        for v in volumes:
            args += ["-v", v]
        args += [f'--{p}' for p in extra_parameters]
        args += [
            "-p", "127.0.0.1:8192:8192", "-p", "127.0.0.1:4040-4050:4040-4050",
            Docker.DOCKER_IMAGE
        ]

        awscreds = read_awscreds(profile=aws_profile)

        subprocess.run(args, check=True, env={
            **os.environ,
            "AWS_ACCESS_KEY_ID": awscreds["access_key_id"],
            "AWS_SECRET_ACCESS_KEY": awscreds["secret_access_key"]})

    def docker_build(self):
        shutil.rmtree("target/deployment/docker/notebooks", ignore_errors=True)
        shutil.copytree("deployment/docker/notebooks", "target/deployment/docker/notebooks")

        os.makedirs("target/deployment/docker/notebooks/lib", exist_ok=True)
        os.link(
            "bioinformatics/target/scala-2.12/bioinformatics-assembly-0.1.jar",
            "target/deployment/docker/notebooks/lib/bioinformatics.jar")
        os.link(
            "spark-r/target/scala-2.12/spark-r-assembly-0.1.jar",
            "target/deployment/docker/notebooks/lib/spark-r.jar")
        os.link(
            "spark-reflow/target/scala-2.12/spark-reflow-assembly-0.1.jar",
            "target/deployment/docker/notebooks/lib/spark-reflow.jar")

        args = [
            "docker", "build",
            "-t", Docker.DOCKER_IMAGE,
            "target/deployment/docker/notebooks"
        ]
        subprocess.run(args, check=True, env=self._env)


class Dev:
    """
    All the commands related to local development.
    """

    def package(self):
        subprocess.run(["sbt", "assembly"], check=True)

    def test(self):
        # We do not test reflow as this would require some AWS resources.
        subprocess.run(["sbt", "; project spark-r; test; project bioinformatics; test"], check=True)

    def format(self):
        subprocess.run(["sbt", "scalafmtAll"], check=True)

    def format_check(self):
        subprocess.run(["sbt", "scalafmtCheckAll"], check=True)


class Make:
    """
    Command-Line Interface.
    """

    def __init__(self):
        parser = argparse.ArgumentParser(
            description='Make health-dataset-generator',
            usage="make <command> [<args>]")
        subcommands = [
            attr for attr in dir(self)
            if not attr.startswith("_") and callable(getattr(self, attr))
        ]
        parser.add_argument(
            'command',
            help='Subcommand to run: one of ' + " ".join(subcommands))
        args = parser.parse_args(sys.argv[1:2])
        if not hasattr(self, args.command):
            print('Unrecognized command')
            parser.print_help()
            exit(1)
        getattr(self, args.command)()

    def docker_run(self):
        parser = argparse.ArgumentParser(description='Run the docker image')
        parser.add_argument(
            '-sbt',
            dest='sbt',
            action='store_true',
            help='run sbt and build the image beforehand')
        parser.add_argument(
            '-b',
            '--build',
            dest='build',
            action='store_true',
            help='build the image beforehand')
        parser.add_argument(
            '-v', '--volume', action='append', help='A volume to mount')
        parser.add_argument(
            '--docker',
            action='append',
            help='Extra parameter to pass to "docker run". Example: ' +
                 '--docker=net=app_default')
        parser.add_argument(
            '--aws_profile',
            help='Profile for AWS credentials',
            default='default')
        args = parser.parse_args(sys.argv[2:])
        self._docker().docker_run(
            build=args.build,
            sbt=args.sbt,
            volumes=args.volume or [],
            extra_parameters=args.docker or [],
            aws_profile=args.aws_profile)

    def package(self):
        parser = argparse.ArgumentParser(description='Package code')
        args = parser.parse_args(sys.argv[2:])
        self._dev().package()

    def docker_build(self):
        parser = argparse.ArgumentParser(description='Build the docker image')
        args = parser.parse_args(sys.argv[2:])
        self._docker().docker_build()

    def test(self):
        parser = argparse.ArgumentParser(description="Execute tests")
        args = parser.parse_args(sys.argv[2:])
        self._dev().test()

    def format(self):
        parser = argparse.ArgumentParser(description='Format source files')
        args = parser.parse_args(sys.argv[2:])
        self._dev().format()

    def format_check(self):
        parser = argparse.ArgumentParser(
            description='Check source file formatting')
        args = parser.parse_args(sys.argv[2:])
        self._dev().format_check()

    def _docker(self):
        return Docker(self._dev())

    def _dev(self):
        return Dev()


if __name__ == "__main__":
    try:
        Make()
    except KeyboardInterrupt as e:
        print("Interrupted")
        sys.exit(1)

pipeline {
  agent {
    node {
      label 'maven'
    }

  }

  environment {
          DOCKER_CREDENTIAL_ID = 'dockerhub-id'
          GITEE_CREDENTIAL_ID = 'gitee-id'
          KUBECONFIG_CREDENTIAL_ID = 'demo-kubeconfig'
          REGISTRY = 'docker.io'
          DOCKERHUB_NAMESPACE = 'branshlee'
          GITEE_ACCOUNT = 'branshLEE'
  }

  parameters {
          string(name:'PROJECT_VERSION',defaultValue: '',description:'')
          string(name:'PROJECT_NAME',defaultValue: '',description:'')
  }

  stages {
    stage('拉取gitee代码') {
      steps {
        git(url: 'https://gitee.com/branshlee/LMarket.git', credentialsId: 'gitee-id', branch: 'main', changelog: true, poll: false)
        sh 'echo 正在构建 $PROJECT_NAME，版本号：$PROJECT_VERSION，提交给$REGISTRY镜像仓库'
      }
    }
  }

}
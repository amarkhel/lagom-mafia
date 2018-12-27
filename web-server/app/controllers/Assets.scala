package controllers

import utils.ErrorHandler
import controllers.Assets.Asset
import javax.inject.Inject

class MyAssets @Inject() (val errorHandler: ErrorHandler, meta:AssetsMetadata) extends AssetsBuilder(errorHandler, meta) {
  def resource(path: String, file: Asset) = versioned(path, file)
}